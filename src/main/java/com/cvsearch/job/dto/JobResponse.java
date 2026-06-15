package com.cvsearch.job.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record JobResponse(
		@NotNull Long id,
		@NotBlank String title,
		@NotNull Long companyId,
		@NotBlank String companyName,
		@NotBlank String description,
		@NotNull String status,
		@NotNull LocalDate appliedDate,
		@NotNull Boolean saved,
		String employmentType) {
}
