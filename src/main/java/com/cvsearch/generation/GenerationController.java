package com.cvsearch.generation;

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

import com.cvsearch.ai.AiService;
import com.cvsearch.generation.dto.PromptResponse;
import com.cvsearch.generation.dto.TailoredProfileRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/jobs")
public class GenerationController {

    private final CvPromptService promptService;
    private final CvPdfService pdfService;
    private final AiService aiService;
    private final ObjectMapper objectMapper;

    public GenerationController(CvPromptService promptService, CvPdfService pdfService,
                                AiService aiService, ObjectMapper objectMapper) {
        this.promptService = promptService;
        this.pdfService = pdfService;
        this.aiService = aiService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/{jobId}/cv-prompt")
    public ResponseEntity<PromptResponse> getCvPrompt(
            @PathVariable Long jobId,
            @RequestParam(defaultValue = "1") Long userId) {

        String prompt = promptService.buildCvPrompt(jobId, userId);
        return ResponseEntity.ok(new PromptResponse(prompt, "cv", jobId, userId));
    }

    @GetMapping("/{jobId}/cover-letter-prompt")
    public ResponseEntity<PromptResponse> getCoverLetterPrompt(
            @PathVariable Long jobId,
            @RequestParam(defaultValue = "1") Long userId) {

        String prompt = promptService.buildCoverLetterPrompt(jobId, userId);
        return ResponseEntity.ok(new PromptResponse(prompt, "cover-letter", jobId, userId));
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
            @RequestBody TailoredProfileRequest tailoredProfile) {

        byte[] pdf = pdfService.generateTailoredCvPdf(jobId, userId, tailoredProfile, template);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"cv-" + jobId + ".pdf\"")
                .body(pdf);
    }

    // ---------- AI-powered endpoints ----------

    @GetMapping("/{jobId}/cv/ai")
    public ResponseEntity<byte[]> generateCvWithAi(
            @PathVariable Long jobId,
            @RequestParam(defaultValue = "1") Long userId,
            @RequestParam(defaultValue = "sidebar") String template) {

        String prompt = promptService.buildCvPrompt(jobId, userId);
        String aiResponse = aiService.chat(prompt);
        TailoredProfileRequest tailored = parseCvJson(aiResponse);

        byte[] pdf = pdfService.generateTailoredCvPdf(jobId, userId, tailored, template);

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

    private TailoredProfileRequest parseCvJson(String text) {
        // Strip markdown code blocks if present (```json ... ``` or ``` ... ```)
        String json = text;
        if (json.contains("```")) {
            json = json.replaceAll("(?s)```(?:json)?\\s*", "").trim();
        }
        try {
            return objectMapper.readValue(json, TailoredProfileRequest.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse AI response as JSON. Response was:\n" + text, e);
        }
    }
}
