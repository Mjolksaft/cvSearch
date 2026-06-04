package com.cvsearch.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record JobResponse(
		@NotNull Long id,
		@NotBlank String title,
		@NotBlank String company,
		@NotBlank String description,
		@NotNull String status) {
}