package com.cvsearch.job.dto;

import jakarta.validation.constraints.NotBlank;

public record BulkJobItem(
        @NotBlank String title,
        @NotBlank String companyName,
        String location,
        String website,
        String description,
        Long externalId) {
}
