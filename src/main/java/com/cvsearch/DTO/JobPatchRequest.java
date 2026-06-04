package com.cvsearch.DTO;

import java.time.LocalDate;

public record JobPatchRequest(
        String title,
        String company,
        String description,
        String status,
        LocalDate appliedDate) {
}