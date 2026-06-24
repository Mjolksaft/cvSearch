package com.cvsearch.generation.dto;

import java.util.List;

/**
 * Step 4 output: the final CV profile (summary + skills).
 * Education, languages, certifications are injected from DB.
 */
public record ProfileOutput(
        String summary,
        List<String> skills) {
}
