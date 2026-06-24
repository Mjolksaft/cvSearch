package com.cvsearch.generation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.cvsearch.ai.AiService;
import com.cvsearch.generation.dto.JobRequirements;
import com.cvsearch.generation.dto.ProfileOutput;
import com.cvsearch.generation.dto.PromptResponse;
import com.cvsearch.generation.dto.TailoredProfileRequest;
import com.cvsearch.generation.dto.TailoredProfileRequest.EducationEntry;
import com.cvsearch.generation.dto.TailoredProfileRequest.ProjectEntry;
import com.cvsearch.userProfile.UserProfile;
import com.cvsearch.userProfile.UserProfileRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityNotFoundException;

@RestController
@RequestMapping("/api/jobs")
public class GenerationController {

    private static final Logger log = LoggerFactory.getLogger(GenerationController.class);

    private final CvPromptService promptService;
    private final CvPdfService pdfService;
    private final AiService aiService;
    private final ObjectMapper objectMapper;
    private final UserProfileRepository profileRepository;
    private final ProjectScorer projectScorer;
    private final AiContentService aiContentService;

    public GenerationController(CvPromptService promptService, CvPdfService pdfService,
                                AiService aiService, ObjectMapper objectMapper,
                                UserProfileRepository profileRepository,
                                ProjectScorer projectScorer,
                                AiContentService aiContentService) {
        this.promptService = promptService;
        this.pdfService = pdfService;
        this.aiService = aiService;
        this.objectMapper = objectMapper;
        this.profileRepository = profileRepository;
        this.projectScorer = projectScorer;
        this.aiContentService = aiContentService;
    }

    @GetMapping("/{jobId}/cv-prompt")
    public ResponseEntity<PromptResponse> getCvPrompt(
            @PathVariable Long jobId,
            @RequestParam(defaultValue = "1") Long userId) {

        // For the prompt preview, pass all project names so you can see the full prompt
        List<String> allProjectNames = getAllProjectNames(userId);
        String prompt = promptService.buildCvPrompt(jobId, userId, allProjectNames);
        return ResponseEntity.ok(new PromptResponse(prompt, "cv", jobId, userId));
    }

    @GetMapping("/{jobId}/cover-letter-prompt")
    public ResponseEntity<PromptResponse> getCoverLetterPrompt(
            @PathVariable Long jobId,
            @RequestParam(defaultValue = "1") Long userId) {

        String prompt = promptService.buildCoverLetterPrompt(jobId, userId);
        return ResponseEntity.ok(new PromptResponse(prompt, "cover-letter", jobId, userId));
    }

    @GetMapping("/{jobId}/project-selection-prompt")
    public ResponseEntity<PromptResponse> getProjectSelectionPrompt(
            @PathVariable Long jobId,
            @RequestParam(defaultValue = "1") Long userId) {

        String prompt = promptService.buildProjectSelectionPrompt(jobId, userId);
        return ResponseEntity.ok(new PromptResponse(prompt, "project-selection", jobId, userId));
    }

    @GetMapping("/{jobId}/extract-requirements-prompt")
    public ResponseEntity<PromptResponse> getExtractRequirementsPrompt(
            @PathVariable Long jobId) {

        String prompt = promptService.buildExtractRequirementsPrompt(jobId);
        return ResponseEntity.ok(new PromptResponse(prompt, "extract-requirements", jobId, 0L));
    }

    @GetMapping("/{jobId}/select-projects-prompt")
    public ResponseEntity<PromptResponse> getSelectProjectsPrompt(
            @PathVariable Long jobId,
            @RequestParam(defaultValue = "1") Long userId) {

        UserProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Profile not found for user id: " + userId));

        // Build a dummy requirements object for the preview
        JobRequirements dummyReq = new JobRequirements(
                List.of("Java", "Spring Boot", "REST API"),
                List.of("Docker", "PostgreSQL"));
        String projectSummaries = promptService.formatProjectSummariesForPrompt(profile.getProjects());
        String prompt = promptService.buildSelectProjectsPrompt(toJson(dummyReq), projectSummaries);
        return ResponseEntity.ok(new PromptResponse(prompt, "select-projects", jobId, userId));
    }

    @GetMapping("/{jobId}/project-scores")
    public ResponseEntity<Map<String, Object>> getProjectScores(
            @PathVariable Long jobId,
            @RequestParam(defaultValue = "1") Long userId) {

        // Step 1: extract requirements from the job (AI call)
        String reqPrompt = promptService.buildExtractRequirementsPrompt(jobId);
        String reqResponse = aiService.chat(reqPrompt);
        JobRequirements requirements = parseJobRequirements(reqResponse);

        // Get the user's profile
        UserProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Profile not found for user id: " + userId));

        // Score all projects (no filtering — shows raw scores)
        List<ProjectScorer.ProjectScore> scores = projectScorer.scoreAll(requirements, profile.getProjects());

        // Sort by score descending for easy reading
        List<ProjectScorer.ProjectScore> sorted = scores.stream()
                .sorted(java.util.Comparator.comparingDouble(ProjectScorer.ProjectScore::score).reversed())
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("requirements", requirements);
        response.put("minimumScore", ProjectScorer.MINIMUM_SCORE);
        response.put("projects", sorted);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{jobId}/cv.pdf")
    public ResponseEntity<byte[]> downloadCvPdf(
            @PathVariable Long jobId,
            @RequestParam(defaultValue = "1") Long userId,
            @RequestParam(defaultValue = "standard") String template) {

        byte[] pdf = pdfService.generateCvPdf(jobId, userId, template);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"cv-" + jobId + ".pdf\"")
                .body(pdf);
    }

    @PostMapping("/{jobId}/cv")
    public ResponseEntity<byte[]> downloadTailoredCvPdf(
            @PathVariable Long jobId,
            @RequestParam(defaultValue = "1") Long userId,
            @RequestParam(defaultValue = "sidebar") String template,
            @RequestParam(defaultValue = "professional-blue") String scheme,
            @RequestBody TailoredProfileRequest tailoredProfile) {

        byte[] pdf = pdfService.generateTailoredCvPdf(jobId, userId, tailoredProfile, template, scheme);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"cv-" + jobId + ".pdf\"")
                .body(pdf);
    }

    // ---------- Preview endpoint (HTML, not PDF) ----------

    @GetMapping("/{jobId}/cv/preview")
    public ResponseEntity<String> previewCvHtml(
            @PathVariable Long jobId,
            @RequestParam(defaultValue = "1") Long userId,
            @RequestParam(defaultValue = "sidebar") String template,
            @RequestParam(defaultValue = "false") boolean debug) {

        String html = pdfService.generateHtml(jobId, userId, template, debug);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }

    // ---------- AI-powered endpoints ----------

    @GetMapping("/{jobId}/cv/ai")
    public ResponseEntity<byte[]> generateCvWithAi(
            @PathVariable Long jobId,
            @RequestParam(defaultValue = "1") Long userId,
            @RequestParam(defaultValue = "classic") String scheme,
            @RequestParam(defaultValue = "sidebar") String template) {

        TailoredProfileRequest content = aiContentService.generateContent(jobId, userId);
        byte[] pdf = pdfService.generateTailoredCvPdf(jobId, userId, content, template, scheme);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"cv-" + jobId + ".pdf\"")
                .body(pdf);
    }

    /* Edit page moved to PageController */

    @GetMapping("/{jobId}/cover-letter/ai")
    public ResponseEntity<String> generateCoverLetterWithAi(
            @PathVariable Long jobId,
            @RequestParam(defaultValue = "1") Long userId) {

        String prompt = promptService.buildCoverLetterPrompt(jobId, userId);
        String response = aiService.chat(prompt);

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"cover-letter-" + jobId + ".txt\"")
                .body(response);
    }

    /** Parse AI response as a JSON array of strings (e.g. project names). */
    private List<String> parseStringListJson(String text) {
        String json = extractJson(text);
        try {
            return objectMapper.readValue(json,
                    new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
        } catch (Exception e) {
            // Try extracting anything that looks like quoted strings
            List<String> fallback = new java.util.ArrayList<>();
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"([^\"]+)\"").matcher(json);
            while (m.find()) {
                fallback.add(m.group(1));
            }
            if (!fallback.isEmpty()) return fallback;
            throw new RuntimeException("Failed to parse AI project selection as JSON array. Response was:\n" + text, e);
        }
    }

    private TailoredProfileRequest parseCvJson(String text) {
        String json = extractJson(text);
        try {
            return objectMapper.readValue(json, TailoredProfileRequest.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse AI response as JSON. Response was:\n" + text, e);
        }
    }

    /** Parse AI response (Step 1) into structured job requirements. */
    private JobRequirements parseJobRequirements(String text) {
        String json = extractJson(text);
        try {
            return objectMapper.readValue(json, JobRequirements.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse job requirements. Response was:\n" + text, e);
        }
    }

    /** Parse AI response (Step 3) into a list of rewritten project entries. */
    private List<ProjectEntry> parseProjectEntries(String text) {
        String json = extractJson(text);
        // Try Jackson first
        try {
            return objectMapper.readValue(json,
                    new TypeReference<List<ProjectEntry>>() {});
        } catch (Exception e) {
            // Jackson failed — try manual regex extraction (more robust)
            log.warn("Jackson failed to parse Step 3 response, trying manual extraction...");
            try {
                return extractProjectsManually(json);
            } catch (Exception e2) {
                throw new RuntimeException("Failed to parse rewritten projects. Response was:\n" + text, e2);
            }
        }
    }

    /**
     * Manually extract project entries from the AI response using regex.
     * This handles cases where the AI returns malformed JSON (e.g. missing
     * quotes on highlight items, bullet-point text instead of arrays, etc.).
     */
    private List<ProjectEntry> extractProjectsManually(String text) {
        List<ProjectEntry> result = new java.util.ArrayList<>();

        // Find each project block: { ... }
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
            // Parse technologies
            List<String> techs = java.util.Arrays.stream(m.group(3).split(","))
                    .map(t -> t.trim().replaceAll("^\"|\"$", ""))
                    .filter(t -> !t.isEmpty())
                    .toList();
            // Parse highlights — handles arrays, bullet lists, comma-separated etc.
            List<String> highlights = extractHighlightsManually(m.group(4));
            result.add(new ProjectEntry(name, desc, techs, highlights));
        }

        if (result.isEmpty()) {
            throw new RuntimeException("Could not extract any projects from response");
        }
        return result;
    }

    /**
     * Extract highlight strings from the highlights section of the AI response.
     * Handles various malformed formats the AI may produce.
     */
    private List<String> extractHighlightsManually(String highlightsText) {
        // Remove brackets if present
        String cleaned = highlightsText.trim()
                .replaceAll("^\\[", "")
                .replaceAll("\\]$", "")
                .trim();

        // If it contains bullet-point characters (•, -, *), split by those
        if (cleaned.matches(".*[•\\-*].*")) {
            return java.util.Arrays.stream(cleaned.split("\\s*[•\\-*]\\s*"))
                    .map(h -> h.trim()
                            .replaceAll("^[\"\\\\]+|[\"\\\\]+$", "")
                            .replaceAll("\\\\", "")
                            .replaceAll(",+$", "")
                            .trim())
                    .filter(h -> !h.isEmpty())
                    .toList();
        }

        // Split by commas, then clean each item
        return java.util.Arrays.stream(cleaned.split(","))
                .map(h -> h.trim()
                        .replaceAll("^[\"\\\\]+|[\"\\\\]+$", "")
                        .replaceAll("\\\\", "")
                        .trim())
                .filter(h -> !h.isEmpty())
                .toList();
    }

    /** Convert highlights (String or List) into a List<String>. */
    @SuppressWarnings("unchecked")
    private List<String> parseHighlights(Object highlights) {
        if (highlights == null) return List.of();
        if (highlights instanceof List) return (List<String>) highlights;
        if (highlights instanceof String s) {
            return Arrays.stream(s.split(","))
                    .map(String::trim)
                    .filter(h -> !h.isEmpty())
                    .toList();
        }
        return List.of();
    }

    /** Parse AI response (Step 4) into profile output (summary + skills). */
    private ProfileOutput parseProfileOutput(String text) {
        String json = extractJson(text);
        try {
            return objectMapper.readValue(json, ProfileOutput.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse profile output. Response was:\n" + text, e);
        }
    }

    /** Convert an object to its JSON string representation. */
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize to JSON", e);
        }
    }

    /** Strip markdown code block fences from AI output. */
    private static String stripCodeBlocks(String text) {
        if (text == null || !text.contains("```")) return text;
        return text.replaceAll("(?s)```(?:json)?\\s*", "").trim();
    }

    /**
     * Extract the first JSON object or array from text, ignoring any
     * preamble or trailing commentary the AI may add.
     */
    private static String extractJson(String text) {
        String cleaned = stripCodeBlocks(text).trim();
        // Find the first { or [
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

    /** Truncate text to at most {@code maxWords} words. */
    private static String limitWords(String text, int maxWords) {
        if (text == null || text.isBlank()) return "";
        String[] words = text.trim().split("\\s+");
        if (words.length <= maxWords) return text.trim();
        return Arrays.stream(words, 0, maxWords).collect(Collectors.joining(" ")) + "…";
    }

    /** Parse education from the stored profile JSON string into EducationEntry records. */
    private List<EducationEntry> parseEducationFromProfile(UserProfile profile) {
        if (profile.getEducation() == null || profile.getEducation().isBlank()) {
            return List.of();
        }
        try {
            com.fasterxml.jackson.core.type.TypeReference<List<com.cvsearch.generation.dto.TailoredProfileRequest.EducationEntry>> typeRef =
                new com.fasterxml.jackson.core.type.TypeReference<>() {};
            return objectMapper.readValue(profile.getEducation(), typeRef);
        } catch (Exception e) {
            // Try parsing as list of maps as fallback
            try {
                List<java.util.Map<String, Object>> raw = objectMapper.readValue(
                        profile.getEducation(),
                        new com.fasterxml.jackson.core.type.TypeReference<List<java.util.Map<String, Object>>>() {});
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

    /** Parse a JSON array of strings from the stored profile. */
    private List<String> parseJsonStringList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json,
                    new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private static String stringOr(Object value) {
        return value != null ? value.toString() : "";
    }

    /** Get all project names from the user's profile (for the prompt preview). */
    private List<String> getAllProjectNames(Long userId) {
        try {
            UserProfile profile = profileRepository.findByUserId(userId)
                    .orElseThrow(() -> new EntityNotFoundException("Profile not found for user id: " + userId));
            if (profile.getProjects() == null || profile.getProjects().isBlank()) {
                return List.of();
            }
            List<java.util.Map<String, Object>> projects = objectMapper.readValue(
                    profile.getProjects(),
                    new com.fasterxml.jackson.core.type.TypeReference<List<java.util.Map<String, Object>>>() {});
            return projects.stream()
                    .map(p -> String.valueOf(p.getOrDefault("name", "")))
                    .filter(n -> !n.isBlank())
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * Filter the AI's selected highlights to only include ones that exist in
     * the original profile. The AI may choose a subset and reorder them, but
     * it must NOT invent new highlights. Any invented ones are removed.
     */
    private List<ProjectEntry> enforceOriginalHighlights(
            List<ProjectEntry> aiProjects, String projectsJson) {
        if (projectsJson == null || projectsJson.isBlank()) return aiProjects;
        try {
            List<java.util.Map<String, Object>> originals = objectMapper.readValue(projectsJson,
                    new com.fasterxml.jackson.core.type.TypeReference<List<java.util.Map<String, Object>>>() {});
            // Build lookup set: project name → set of original highlights
            java.util.Map<String, java.util.Set<String>> originalHighlightsSet = new java.util.HashMap<>();
            for (var orig : originals) {
                String name = String.valueOf(orig.getOrDefault("name", "")).trim().toLowerCase();
                @SuppressWarnings("unchecked")
                List<String> hls = orig.get("highlights") instanceof List
                        ? (List<String>) orig.get("highlights")
                        : List.of();
                originalHighlightsSet.put(name, new java.util.HashSet<>(hls));
            }
            // Filter AI highlights: keep only those that exist in the originals
            // Use normalized (lowercase, trimmed) comparison to handle minor differences
            return aiProjects.stream().map(p -> {
                String key = p.name() != null ? p.name().trim().toLowerCase() : "";
                java.util.Set<String> originalsForProject = originalHighlightsSet.getOrDefault(key, java.util.Set.of());
                java.util.Set<String> originalsNormalized = originalsForProject.stream()
                        .map(h -> h.trim().toLowerCase())
                        .collect(java.util.stream.Collectors.toSet());
                List<String> filtered = p.highlights().stream()
                        .filter(h -> originalsNormalized.contains(h.trim().toLowerCase()))
                        .toList();
                // If all were filtered out (shouldn't happen), fall back to all originals
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

    /**
     * Overwrite the AI's generated technologies with the originals from the profile.
     * This prevents the AI from inventing technologies that don't exist in the project.
     */
    private List<ProjectEntry> enforceOriginalTechnologies(
            List<ProjectEntry> aiProjects, String projectsJson) {
        if (projectsJson == null || projectsJson.isBlank()) return aiProjects;
        try {
            List<java.util.Map<String, Object>> originals = objectMapper.readValue(projectsJson,
                    new com.fasterxml.jackson.core.type.TypeReference<List<java.util.Map<String, Object>>>() {});
            // Build lookup map: project name (lowercase) → technologies
            java.util.Map<String, List<String>> techMap = new java.util.HashMap<>();
            for (var orig : originals) {
                String name = String.valueOf(orig.getOrDefault("name", "")).trim().toLowerCase();
                @SuppressWarnings("unchecked")
                List<String> techs = orig.get("technologies") instanceof List
                        ? (List<String>) orig.get("technologies")
                        : List.of();
                techMap.put(name, techs);
            }
            // Replace technologies in AI output
            return aiProjects.stream().map(p -> {
                String key = p.name() != null ? p.name().trim().toLowerCase() : "";
                List<String> originalTechs = techMap.getOrDefault(key, p.technologies());
                return new ProjectEntry(p.name(), p.description(), originalTechs, p.highlights());
            }).toList();
        } catch (Exception e) {
            return aiProjects;
        }
    }
}
