package com.cvsearch.generation;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cvsearch.ai.AiService;
import com.cvsearch.generation.dto.JobRequirements;
import com.cvsearch.generation.dto.ProfileOutput;
import com.cvsearch.generation.dto.TailoredProfileRequest;
import com.cvsearch.generation.dto.TailoredProfileRequest.EducationEntry;
import com.cvsearch.generation.dto.TailoredProfileRequest.ProjectEntry;
import com.cvsearch.userProfile.UserProfile;
import com.cvsearch.userProfile.UserProfileRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityNotFoundException;


@Service
public class AiContentService {

    private static final Logger log = LoggerFactory.getLogger(AiContentService.class);

    private final CvPromptService promptService;
    private final AiService aiService;
    private final ObjectMapper objectMapper;
    private final UserProfileRepository profileRepository;
    private final ProjectScorer projectScorer;

    public AiContentService(CvPromptService promptService, AiService aiService,
                            ObjectMapper objectMapper, UserProfileRepository profileRepository,
                            ProjectScorer projectScorer) {
        this.promptService = promptService;
        this.aiService = aiService;
        this.objectMapper = objectMapper;
        this.profileRepository = profileRepository;
        this.projectScorer = projectScorer;
    }

    public TailoredProfileRequest generateContent(Long jobId, Long userId) {
        UserProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Profile not found for user id: " + userId));

        
        String reqPrompt = promptService.buildExtractRequirementsPrompt(jobId);
        String reqResponse = aiService.chat(reqPrompt);
        JobRequirements requirements = parseJobRequirements(reqResponse);

        
        List<ProjectScorer.ProjectScore> allScores = projectScorer.scoreAll(
                requirements, profile.getProjects());

        log.info("=== Project scores for job {} ===", jobId);
        log.info("Requirements: required={}, preferred={}", requirements.required(), requirements.preferred());
        log.info("Minimum score threshold: {}", ProjectScorer.MINIMUM_SCORE);
        allScores.stream()
                .sorted(java.util.Comparator.comparingDouble(ProjectScorer.ProjectScore::score).reversed())
                .forEach(s -> log.info("  {} → {} (selected: {})",
                        s.name(), s.score(), s.score() >= ProjectScorer.MINIMUM_SCORE));
        log.info("==============================");

        List<String> selectedProjectNames = projectScorer.selectTopProjects(
                requirements, profile.getProjects(), 3);

        if (selectedProjectNames.isEmpty()) {
            throw new RuntimeException("No projects found in profile — add at least one project to generate a CV.");
        }

        
        String selectedProjectsFull = promptService.formatSelectedProjectsForPrompt(
                profile.getProjects(), selectedProjectNames);

        Map<String, Double> selectedScoreMap = allScores.stream()
                .filter(s -> selectedProjectNames.contains(s.name()))
                .collect(Collectors.toMap(ProjectScorer.ProjectScore::name, ProjectScorer.ProjectScore::score));
        double totalScore = selectedScoreMap.values().stream().mapToDouble(d -> d).sum();
        int totalBudget = 250;
        int maxPerProject = 120;
        int minPerProject = 60;
        String spaceAllocation = selectedProjectNames.stream()
                .map(name -> {
                    double pct = selectedScoreMap.get(name) / totalScore;
                    int words = (int) Math.round(pct * totalBudget);
                    words = Math.min(maxPerProject, Math.max(minPerProject, words));
                    int lower = (int) Math.round(words * 0.8);
                    int upper = Math.min(maxPerProject, (int) Math.round(words * 1.2));
                    return String.format("- %s (relevance %.0f%%) → description %d-%d words, highlights unlimited",
                            name, pct * 100, lower, upper);
                })
                .collect(Collectors.joining("\n"));

        String rewritePrompt = promptService.buildRewriteProjectsPrompt(
                toJson(requirements), selectedProjectsFull, spaceAllocation);
        String rewriteResponse = aiService.chat(rewritePrompt);
        List<ProjectEntry> rewrittenProjects = parseProjectEntries(rewriteResponse);

        
        rewrittenProjects = enforceOriginalTechnologies(rewrittenProjects, profile.getProjects());
        rewrittenProjects = enforceOriginalHighlights(rewrittenProjects, profile.getProjects());

        
        Map<String, Double> scoreMap = allScores.stream()
                .collect(Collectors.toMap(
                        ProjectScorer.ProjectScore::name,
                        ProjectScorer.ProjectScore::score));
        var mutableProjects = new java.util.ArrayList<>(rewrittenProjects);
        mutableProjects.sort((a, b) -> {
            double scoreA = scoreMap.getOrDefault(a.name(), 0.0);
            double scoreB = scoreMap.getOrDefault(b.name(), 0.0);
            return Double.compare(scoreB, scoreA);
        });
        rewrittenProjects = mutableProjects;

        
        String finalPrompt = promptService.buildFinalProfilePrompt(
                toJson(requirements),
                toJson(rewrittenProjects),
                safe(profile.getSummary()),
                safe(profile.getSkills()),
                safe(profile.getEducation()),
                safe(profile.getLanguages()),
                safe(profile.getCertifications()),
                safe(profile.getCoursework()));
        String finalResponse = aiService.chat(finalPrompt);
        ProfileOutput profileOutput = parseProfileOutput(finalResponse);

        
        String summary = profileOutput.summary() != null ? limitWords(profileOutput.summary(), 50) : "";
        List<String> skills = profileOutput.skills() != null
                ? profileOutput.skills().stream().limit(10).toList()
                : List.of();

        List<EducationEntry> staticEducation = parseEducationFromProfile(profile);
        List<String> staticLanguages = parseJsonStringList(profile.getLanguages());
        List<String> staticCertifications = parseJsonStringList(profile.getCertifications());

        List<String> coursework = parseJsonStringList(profile.getCoursework()).stream()
                .limit(5)
                .toList();

        return new TailoredProfileRequest(
                summary, skills, rewrittenProjects,
                staticEducation, staticLanguages, staticCertifications, coursework);
    }

    

    private JobRequirements parseJobRequirements(String text) {
        String json = extractJson(text);
        try {
            return objectMapper.readValue(json, JobRequirements.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse job requirements. Response was:\n" + text, e);
        }
    }

    private List<ProjectEntry> parseProjectEntries(String text) {
        String json = extractJson(text);
        try {
            return objectMapper.readValue(json,
                    new TypeReference<List<ProjectEntry>>() {});
        } catch (Exception e) {
            log.warn("Jackson failed to parse Step 3 response, trying manual extraction...");
            try {
                return extractProjectsManually(json);
            } catch (Exception e2) {
                throw new RuntimeException("Failed to parse rewritten projects. Response was:\n" + text, e2);
            }
        }
    }

    private List<ProjectEntry> extractProjectsManually(String text) {
        List<ProjectEntry> result = new java.util.ArrayList<>();
        java.util.regex.Pattern projectPattern = java.util.regex.Pattern.compile(
            "\\{\\s*\"name\"\\s*:\\s*\"([^\"]+)\"\\s*,"
            + "\\s*\"description\"\\s*:\\s*\"([^\"]+)\"\\s*,"
            + "\\s*\"technologies\"\\s*:\\s*\\[([^\\]]+)\\]\\s*,"
            + "\\s*\"highlights\"\\s*:\\s*([^\\}]+)\\s*\\}",
            java.util.regex.Pattern.DOTALL);
        java.util.regex.Matcher m = projectPattern.matcher(text);
        while (m.find()) {
            String name = m.group(1).trim();
            String desc = m.group(2).trim();
            List<String> techs = java.util.Arrays.stream(m.group(3).split(","))
                    .map(t -> t.trim().replaceAll("^\"|\"$", ""))
                    .filter(t -> !t.isEmpty())
                    .toList();
            List<String> highlights = extractHighlightsManually(m.group(4));
            result.add(new ProjectEntry(name, desc, techs, highlights));
        }
        if (result.isEmpty()) {
            throw new RuntimeException("Could not extract any projects from response");
        }
        return result;
    }

    private List<String> extractHighlightsManually(String highlightsText) {
        String cleaned = highlightsText.trim()
                .replaceAll("^\\[", "")
                .replaceAll("\\]$", "")
                .trim();
        if (cleaned.matches(".*[•\\-*].*")) {
            return java.util.Arrays.stream(cleaned.split("\\s*[•\\-*]\\s*"))
                    .map(h -> h.trim().replaceAll("^[\"\\\\]+|[\"\\\\]+$", "").replaceAll("\\\\", "").replaceAll(",+$", "").trim())
                    .filter(h -> !h.isEmpty())
                    .toList();
        }
        return java.util.Arrays.stream(cleaned.split(","))
                .map(h -> h.trim().replaceAll("^[\"\\\\]+|[\"\\\\]+$", "").replaceAll("\\\\", "").trim())
                .filter(h -> !h.isEmpty())
                .toList();
    }

    private ProfileOutput parseProfileOutput(String text) {
        String json = extractJson(text);
        try {
            return objectMapper.readValue(json, ProfileOutput.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse profile output. Response was:\n" + text, e);
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize to JSON", e);
        }
    }

    private static String extractJson(String text) {
        String cleaned = text != null ? text.replaceAll("(?s)```(?:json)?\\s*", "").trim() : "";
        int start = -1;
        for (int i = 0; i < cleaned.length(); i++) {
            char c = cleaned.charAt(i);
            if (c == '{' || c == '[') {
                start = i;
                break;
            }
        }
        if (start < 0) {
            throw new RuntimeException("No JSON object or array found in response:\n" + text);
        }
        return cleaned.substring(start);
    }

    private static String safe(String value) {
        return value != null ? value : "";
    }

    private static String limitWords(String text, int maxWords) {
        if (text == null || text.isBlank()) return "";
        String[] words = text.trim().split("\\s+");
        if (words.length <= maxWords) return text.trim();
        return java.util.Arrays.stream(words, 0, maxWords).collect(Collectors.joining(" ")) + "…";
    }

    private List<EducationEntry> parseEducationFromProfile(UserProfile profile) {
        if (profile.getEducation() == null || profile.getEducation().isBlank()) {
            return List.of();
        }
        try {
            TypeReference<List<EducationEntry>> typeRef = new TypeReference<>() {};
            return objectMapper.readValue(profile.getEducation(), typeRef);
        } catch (Exception e) {
            try {
                List<Map<String, Object>> raw = objectMapper.readValue(
                        profile.getEducation(),
                        new TypeReference<List<Map<String, Object>>>() {});
                return raw.stream()
                        .map(m -> new EducationEntry(
                                stringOr(m.get("degree")),
                                stringOr(m.get("school")),
                                stringOr(m.get("year"))))
                        .toList();
            } catch (Exception e2) {
                return List.of();
            }
        }
    }

    private List<String> parseJsonStringList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private static String stringOr(Object value) {
        return value != null ? value.toString() : "";
    }

    private List<ProjectEntry> enforceOriginalTechnologies(
            List<ProjectEntry> aiProjects, String projectsJson) {
        if (projectsJson == null || projectsJson.isBlank()) return aiProjects;
        try {
            List<Map<String, Object>> originals = objectMapper.readValue(projectsJson,
                    new TypeReference<List<Map<String, Object>>>() {});
            Map<String, List<String>> techMap = new java.util.HashMap<>();
            for (var orig : originals) {
                String name = String.valueOf(orig.getOrDefault("name", "")).trim().toLowerCase();
                @SuppressWarnings("unchecked")
                List<String> techs = orig.get("technologies") instanceof List
                        ? (List<String>) orig.get("technologies")
                        : List.of();
                techMap.put(name, techs);
            }
            return aiProjects.stream().map(p -> {
                String key = p.name() != null ? p.name().trim().toLowerCase() : "";
                List<String> originalTechs = techMap.getOrDefault(key, p.technologies());
                return new ProjectEntry(p.name(), p.description(), originalTechs, p.highlights());
            }).toList();
        } catch (Exception e) {
            return aiProjects;
        }
    }

    private List<ProjectEntry> enforceOriginalHighlights(
            List<ProjectEntry> aiProjects, String projectsJson) {
        if (projectsJson == null || projectsJson.isBlank()) return aiProjects;
        try {
            List<Map<String, Object>> originals = objectMapper.readValue(projectsJson,
                    new TypeReference<List<Map<String, Object>>>() {});
            Map<String, java.util.Set<String>> originalHighlightsSet = new java.util.HashMap<>();
            for (var orig : originals) {
                String name = String.valueOf(orig.getOrDefault("name", "")).trim().toLowerCase();
                @SuppressWarnings("unchecked")
                List<String> hls = orig.get("highlights") instanceof List
                        ? (List<String>) orig.get("highlights")
                        : List.of();
                originalHighlightsSet.put(name, new java.util.HashSet<>(hls));
            }
            return aiProjects.stream().map(p -> {
                String key = p.name() != null ? p.name().trim().toLowerCase() : "";
                java.util.Set<String> originalsForProject = originalHighlightsSet.getOrDefault(key, java.util.Set.of());
                java.util.Set<String> originalsNormalized = originalsForProject.stream()
                        .map(h -> h.trim().toLowerCase())
                        .collect(Collectors.toSet());
                List<String> filtered = p.highlights().stream()
                        .filter(h -> originalsNormalized.contains(h.trim().toLowerCase()))
                        .toList();
                if (filtered.isEmpty()) {
                    return new ProjectEntry(p.name(), p.description(), p.technologies(),
                            new java.util.ArrayList<>(originalsForProject));
                }
                return new ProjectEntry(p.name(), p.description(), p.technologies(), filtered);
            }).toList();
        } catch (Exception e) {
            return aiProjects;
        }
    }
}
