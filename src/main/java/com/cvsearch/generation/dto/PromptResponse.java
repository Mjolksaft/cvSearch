package com.cvsearch.generation.dto;

public record PromptResponse(
        String prompt,
        String type,
        Long jobId,
        Long userId) {
}
