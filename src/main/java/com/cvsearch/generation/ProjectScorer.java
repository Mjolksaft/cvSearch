package com.cvsearch.generation;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.cvsearch.generation.dto.JobRequirements;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Scores each project by technology overlap with job requirements.
 * Technologies are grouped by category with different weights:
 *   Languages  >  Frameworks  >  Databases / DevOps  >  Tools / Other
 *
 * Within each category, "required" matches score more than "preferred".
 * Returns projects sorted by score descending so the best match is first.
 */
@Component
public class ProjectScorer {

    private static final int WEIGHT_LANGUAGE  = 4;
    private static final int WEIGHT_FRAMEWORK = 3;
    private static final int WEIGHT_DATABASE  = 2;
    private static final int WEIGHT_DEVOPS    = 2;
    private static final int WEIGHT_TOOL      = 1;
    private static final int WEIGHT_OTHER     = 1;

    private static final double REQUIRED_MULTIPLIER  = 1.0;
    private static final double PREFERRED_MULTIPLIER = 0.5;

    /** 51 62
     * Minimum score for a project to be included.
     * A score of 1.0 means at least one meaningful technology match
     * (e.g. one required tool or one preferred language/framework).
     * Projects with no real overlap score 0.0 and are filtered out.
     */
    public static final double MINIMUM_SCORE = 1.0;

    // ── Category keyword sets (all lowercase) ──

    private static final Set<String> LANGUAGES = Set.of(
            "java", "python", "javascript", "typescript", "kotlin",
            "c#", "c++", "csharp", "cplusplus", "go", "golang", "rust",
            "ruby", "php", "scala", "swift", "dart", "sql",
            "html", "css", "sass", "less", "shell", "bash",
            "powershell", "groovy", "r", "matlab");

    private static final Set<String> FRAMEWORKS = Set.of(
            "spring", "spring boot", "springboot", "spring mvc",
            "react", "react.js", "angular", "angularjs",
            "vue", "vue.js", "vuejs",
            "node", "node.js", "nodejs",
            "express", "express.js",
            "django", "flask", "fastapi",
            "asp.net", ".net", "dotnet", "aspnet",
            "hibernate", "jpa", "mybatis",
            "junit", "mockito", "testng",
            "tensorflow", "pytorch", "keras",
            "thymeleaf", "bootstrap", "tailwind",
            "jquery", "next.js", "nextjs", "nuxt",
            "laravel", "rails", "symfony",
            "svelte", "redux", "graphql",
            "rest", "rest api", "restapi",
            "soap", "grpc",
            "swing", "javafx");

    private static final Set<String> DATABASES = Set.of(
            "postgresql", "postgres", "mysql", "mariadb",
            "oracle", "mongodb", "redis",
            "elasticsearch", "cassandra", "sqlite",
            "mssql", "sql server", "sqlserver",
            "dynamodb", "firebase", "couchdb",
            "neo4j", "influxdb", "nosql");

    private static final Set<String> DEVOPS = Set.of(
            "docker", "kubernetes", "k8s",
            "aws", "azure", "gcp", "google cloud", "googlecloud",
            "terraform", "ansible", "puppet", "chef",
            "jenkins", "github actions", "gitlab ci",
            "ci/cd", "cicd", "pipeline",
            "openshift", "cloud", "microservices",
            "linux", "unix");

    private static final Set<String> TOOLS = Set.of(
            "git", "github", "gitlab", "bitbucket",
            "maven", "gradle", "webpack", "vite",
            "sonar", "sonarqube",
            "jira", "confluence",
            "kafka", "rabbitmq", "activemq",
            "oauth", "jwt",
            "ssl", "tls",
            "agile", "scrum", "kanban",
            "tdd", "bdd",
            "selenium", "cypress",
            "uml", "design patterns",
            "solid", "ddd", "clean code",
            "swagger", "openapi",
            "postman");

    private final ObjectMapper objectMapper;

    public ProjectScorer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // ── Public API ──

    /**
     * Score all projects and return the top N names, sorted by score descending
     * (best match first). Only projects with a score of at least {@link #MINIMUM_SCORE}
     * are included — irrelevant projects are filtered out.
     * Returns an empty list if no project scores high enough.
     */
    public List<String> selectTopProjects(JobRequirements requirements,
                                          String projectsJson,
                                          int maxProjects) {
        if (projectsJson == null || projectsJson.isBlank()) {
            return List.of();
        }

        List<Map<String, Object>> projects = parseProjects(projectsJson);
        if (projects.isEmpty()) return List.of();

        // Normalise requirement lists
        List<String> requiredTechs  = normaliseAll(requirements.required());
        List<String> preferredTechs = normaliseAll(requirements.preferred());

        // Score each project, filter by minimum score, sort descending
        List<ScoredProject> scored = projects.stream()
                .map(p -> scoreProject(p, requiredTechs, preferredTechs))
                .filter(s -> s.score() >= MINIMUM_SCORE)
                .sorted(Comparator.comparingDouble(ScoredProject::score).reversed())
                .toList();

        // Take top N
        int limit = Math.min(maxProjects, scored.size());
        return scored.subList(0, limit).stream()
                .map(ScoredProject::name)
                .toList();
    }

    // ── Per-project scoring ──

    private ScoredProject scoreProject(Map<String, Object> project,
                                        List<String> requiredTechs,
                                        List<String> preferredTechs) {
        String name = String.valueOf(project.getOrDefault("name", "Unknown"));
        List<String> projectTechs = extractTechnologies(project);

        double score = 0.0;

        for (String tech : projectTechs) {
            String normalised = tech.trim().toLowerCase();
            int categoryWeight = categoryWeight(normalised);

            if (requiredTechs.contains(normalised)) {
                score += categoryWeight * REQUIRED_MULTIPLIER;
            } else if (preferredTechs.contains(normalised)) {
                score += categoryWeight * PREFERRED_MULTIPLIER;
            }
            // Fuzzy fallback: check if this tech appears as a whole word in a
            // requirement, or vice versa. Uses word-boundary matching to avoid
            // false positives like "java" matching "javascript".
            else {
                for (String req : requiredTechs) {
                    if (fuzzyMatch(normalised, req)) {
                        score += categoryWeight * REQUIRED_MULTIPLIER * 0.8;
                        break;
                    }
                }
                if (score == 0.0) {
                    for (String pref : preferredTechs) {
                        if (fuzzyMatch(normalised, pref)) {
                            score += categoryWeight * PREFERRED_MULTIPLIER * 0.8;
                            break;
                        }
                    }
                }
            }
        }

        return new ScoredProject(name, score);
    }

    /**
     * Fuzzy match using word boundaries. Returns true if {@code tech} appears
     * as a whole word within {@code keyword}, or vice versa.
     * This prevents false positives like "java" matching "javascript"
     * while still handling cases like "spring" matching "spring boot".
     */
    private static boolean fuzzyMatch(String tech, String keyword) {
        // Exact match
        if (tech.equals(keyword)) return true;
        // Tech as a whole word within the keyword
        if (Pattern.compile("\\b" + Pattern.quote(tech) + "\\b").matcher(keyword).find()) return true;
        // Keyword as a whole word within the tech
        if (Pattern.compile("\\b" + Pattern.quote(keyword) + "\\b").matcher(tech).find()) return true;
        return false;
    }

    // ── Category lookup ──

    private static int categoryWeight(String tech) {
        if (LANGUAGES.contains(tech))  return WEIGHT_LANGUAGE;
        if (FRAMEWORKS.contains(tech)) return WEIGHT_FRAMEWORK;
        if (DATABASES.contains(tech))  return WEIGHT_DATABASE;
        if (DEVOPS.contains(tech))     return WEIGHT_DEVOPS;
        if (TOOLS.contains(tech))      return WEIGHT_TOOL;
        return WEIGHT_OTHER;
    }

    // ── Helpers ──

    @SuppressWarnings("unchecked")
    private List<String> extractTechnologies(Map<String, Object> project) {
        Object techs = project.get("technologies");
        if (techs instanceof List) {
            return ((List<Object>) techs).stream()
                    .map(Object::toString)
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .toList();
        }
        return List.of();
    }

    private List<Map<String, Object>> parseProjects(String json) {
        try {
            return objectMapper.readValue(json,
                    new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<String> normaliseAll(List<String> items) {
        if (items == null) return List.of();
        return items.stream()
                .map(s -> s.trim().toLowerCase())
                .toList();
    }

    // ── Public record for score preview ──

    public record ProjectScore(String name, double score) {}

    /**
     * Score all projects and return every score (unfiltered, unsorted).
     * Used by the preview endpoint for tuning {@link #MINIMUM_SCORE}.
     */
    public List<ProjectScore> scoreAll(JobRequirements requirements,
                                       String projectsJson) {
        if (projectsJson == null || projectsJson.isBlank()) {
            return List.of();
        }

        List<Map<String, Object>> projects = parseProjects(projectsJson);
        if (projects.isEmpty()) return List.of();

        List<String> requiredTechs  = normaliseAll(requirements.required());
        List<String> preferredTechs = normaliseAll(requirements.preferred());

        return projects.stream()
                .map(p -> {
                    ScoredProject sp = scoreProject(p, requiredTechs, preferredTechs);
                    return new ProjectScore(sp.name(), sp.score());
                })
                .toList();
    }

    // ── Internal record ──

    private record ScoredProject(String name, double score) {}
}
