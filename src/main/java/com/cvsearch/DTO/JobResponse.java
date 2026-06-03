package com.cvsearch.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record JobResponse(
    @NotBlank
	String title,
	@NotBlank
	String company,
	@NotBlank
	String description,
	@NotNull
	String status
) {}