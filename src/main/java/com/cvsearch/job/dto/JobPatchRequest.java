package com.cvsearch.job.dto;

import java.time.LocalDate;

public record JobPatchRequest(
        String title,
        Long companyId,
        String description,
        String status,
        LocalDate appliedDate,
        Boolean saved,
        String employmentType,
        String website) {
}
