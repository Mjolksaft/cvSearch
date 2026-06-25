package com.cvsearch.generation;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;


@Component
public class JobDescriptionSummarizer {

    
    private static final List<Pattern> REQUIREMENT_HEADINGS = List.of(
            Pattern.compile("(?im)^\\s*(?:requirements|qualifications|we\\s+are\\s+looking\\s+for|who\\s+you\\s+are|what\\s+we\\s+(?:are\\s+)?looking\\s+for|om\\s+dig|kvalifikationer|krav|vi\\s+söker|vi\\s+letar\\s+efter)\\s*[:\\-]?\\s*$"),
            Pattern.compile("(?im)^\\s*(?:required\\s+skills|skills\\s+required|technical\\s+skills|competencies|key\\s+skills|essential|desirable|meritorious|meriterande|ansvar|ansvarsområden|arbetsuppgifter|roles?\\s+&\\s+responsibilities|what\\s+you(['']?ll)?\\s+(?:do|be\\s+doing|bring))\\s*[:\\-]?\\s*$")
    );

    
    private static final Set<String> TECH_KEYWORDS = Set.of(
            
            "java", "python", "javascript", "typescript", "kotlin", "c#", "c++", "csharp",
            "go", "golang", "rust", "ruby", "php", "scala", "swift", "dart", "sql",
            
            "spring", "spring boot", "react", "angular", "vue", "vue.js", "node.js",
            "express", "django", "flask", "asp.net", ".net", "hibernate", "jpa",
            "mybatis", "junit", "mockito", "tensorflow", "pytorch",
            
            "postgresql", "postgres", "mysql", "oracle", "mongodb", "redis",
            "elasticsearch", "cassandra", "mariadb", "sqlite", "mssql",
            
            "docker", "kubernetes", "k8s", "aws", "azure", "gcp", "google cloud",
            "terraform", "ansible", "jenkins", "github actions", "ci/cd", "pipeline",
            "openshift", "cloud", "microservices",
            
            "rest", "graphql", "api", "git", "linux", "agile", "scrum", "kanban",
            "tdd", "test", "unit test", "integration test", "e2e", "selenium",
            "kafka", "rabbitmq", "jms", "soap", "oauth", "jwt", "ssl", "tls",
            "html", "css", "sass", "less", "webpack", "maven", "gradle",
            "sonar", "sonarqube", "jira", "confluence",
            "uml", "design patterns", "clean code", "solid", "ddd",
            
            "systemutvecklare", "utvecklare", "programmerare", "backend", "frontend",
            "fullstack", "full stack", "utveckling", "systemutveckling"
    );

    private static final int MAX_CHARS = 3000;

    
    public String summarize(String description) {
        if (description == null || description.isBlank()) return "";

        
        String clean = stripHtml(description);

        
        String[] lines = clean.split("\\n");

        
        StringBuilder requirementsSection = new StringBuilder();
        StringBuilder techSentences = new StringBuilder();
        StringBuilder topSection = new StringBuilder();
        boolean inRequirements = false;
        int linesTaken = 0;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                if (inRequirements) {
                    requirementsSection.append("\n");
                }
                continue;
            }

            
            if (isRequirementHeading(trimmed)) {
                inRequirements = true;
                requirementsSection.append("\n--- ").append(trimmed).append(" ---\n");
                continue;
            }

            if (inRequirements) {
                requirementsSection.append(trimmed).append("\n");
            } else if (linesTaken < 10) {
                
                topSection.append(trimmed).append("\n");
                linesTaken++;
            }

            
            if (containsTechKeyword(trimmed)) {
                techSentences.append("• ").append(trimmed).append("\n");
            }
        }

        
        StringBuilder result = new StringBuilder();

        if (!requirementsSection.isEmpty()) {
            result.append(requirementsSection.toString().trim()).append("\n\n");
        }

        if (!techSentences.isEmpty()) {
            result.append("--- Tech mentions ---\n");
            
            String techText = techSentences.toString();
            if (result.length() + techText.length() <= MAX_CHARS) {
                result.append(techText);
            } else {
                int space = MAX_CHARS - result.length();
                if (space > 50) {
                    result.append(techText, 0, Math.min(techText.length(), space));
                }
            }
            result.append("\n\n");
        }

        if (!topSection.isEmpty() && result.length() < MAX_CHARS - 200) {
            result.append("--- Overview ---\n").append(topSection.toString().trim());
        }

        
        String finalText = result.toString().trim();
        if (finalText.length() > MAX_CHARS) {
            finalText = finalText.substring(0, MAX_CHARS) + "\n… [truncated]";
        }

        return finalText;
    }

    private boolean isRequirementHeading(String line) {
        for (Pattern p : REQUIREMENT_HEADINGS) {
            if (p.matcher(line).matches()) return true;
        }
        return false;
    }

    private boolean containsTechKeyword(String text) {
        String lower = text.toLowerCase();
        return TECH_KEYWORDS.stream().anyMatch(lower::contains);
    }

    private String stripHtml(String html) {
        if (html == null) return "";
        
        String text = html.replaceAll("(?s)<[^>]+>", " ");
        
        text = text.replace("&amp;", "&");
        text = text.replace("&lt;", "<");
        text = text.replace("&gt;", ">");
        text = text.replace("&quot;", "\"");
        text = text.replace("&#39;", "'");
        text = text.replace("&nbsp;", " ");
        
        text = text.replaceAll("\\s+", " ").trim();
        
        text = text.replaceAll("[.!?] ", "$0\n");
        return text;
    }
}
