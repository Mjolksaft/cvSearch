package com.cvsearch.DTO;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record JobRequest(
	@NotBlank
	String title,
	@NotBlank
	String company,
	@NotBlank
	String description,
	@NotBlank
	String status,
	@NotNull
	LocalDate appliedDate
) {}