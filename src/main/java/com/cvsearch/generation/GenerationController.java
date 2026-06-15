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

import com.cvsearch.generation.dto.PromptResponse;
import com.cvsearch.generation.dto.TailoredProfileRequest;

@RestController
@RequestMapping("/api/jobs")
public class GenerationController {

    private final CvPromptService promptService;
    private final CvPdfService pdfService;

    public GenerationController(CvPromptService promptService, CvPdfService pdfService) {
        this.promptService = promptService;
        this.pdfService = pdfService;
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
            @RequestParam(defaultValue = "standard") String template,
            @RequestBody TailoredProfileRequest tailoredProfile) {

        byte[] pdf = pdfService.generateTailoredCvPdf(jobId, userId, tailoredProfile, template);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"cv-" + jobId + ".pdf\"")
                .body(pdf);
    }
}
