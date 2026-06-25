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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cvsearch.ai.AiService;
import com.cvsearch.generation.dto.JobRequirements;
import com.cvsearch.generation.dto.PromptResponse;
import com.cvsearch.generation.dto.TailoredProfileRequest;

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

        
        String reqPrompt = promptService.buildExtractRequirementsPrompt(jobId);
        String reqResponse = aiService.chat(reqPrompt);
        JobRequirements requirements = parseJobRequirements(reqResponse);

        
        UserProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Profile not found for user id: " + userId));

        
        List<ProjectScorer.ProjectScore> scores = projectScorer.scoreAll(requirements, profile.getProjects());

        
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

    
    private JobRequirements parseJobRequirements(String text) {
        String json = extractJson(text);
        try {
            return objectMapper.readValue(json, JobRequirements.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse job requirements. Response was:\n" + text, e);
        }
    }

    
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize to JSON", e);
        }
    }

    
    private static String stripCodeBlocks(String text) {
        if (text == null || !text.contains("```")) return text;
        return text.replaceAll("(?s)```(?:json)?\\s*", "").trim();
    }

    
    private static String extractJson(String text) {
        String cleaned = stripCodeBlocks(text).trim();
        
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


}
