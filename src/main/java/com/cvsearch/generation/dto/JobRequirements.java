package com.cvsearch.generation.dto;

import java.util.List;

/**
 * Step 1 output: extracted from the job ad by the AI.
 */
public record JobRequirements(
        List<String> required,
        List<String> preferred) {
}
