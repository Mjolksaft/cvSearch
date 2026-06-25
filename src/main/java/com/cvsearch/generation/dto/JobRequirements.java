package com.cvsearch.generation.dto;

import java.util.List;


public record JobRequirements(
        List<String> required,
        List<String> preferred) {
}
