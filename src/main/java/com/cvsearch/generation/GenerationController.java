package com.cvsearch.generation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cvsearch.generation.dto.PromptResponse;

@RestController
@RequestMapping("/api/jobs")
public class GenerationController {

    private final CvPromptService promptService;

    public GenerationController(CvPromptService promptService) {
        this.promptService = promptService;
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
}
