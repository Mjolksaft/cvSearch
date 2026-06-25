package com.cvsearch.generation.dto;

import java.util.List;


public record ProfileOutput(
        String summary,
        List<String> skills) {
}
