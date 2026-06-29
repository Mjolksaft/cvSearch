package com.cvsearch.job.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record JobRequest(
		@NotBlank String title,
		@NotNull Long companyId,
		@NotBlank String description,
		@NotBlank String status,
		@NotNull LocalDate appliedDate,
		String employmentType,
		String website,
		Double latitude,
		Double longitude) {
}
