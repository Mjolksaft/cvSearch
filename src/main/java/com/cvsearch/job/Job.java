package com.cvsearch.job;

import java.time.LocalDate;

import com.cvsearch.company.Company;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
public class Job {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true)
	private Long externalId;
	@NotBlank
	@Column(length = 500)
	private String title;
	@NotNull
	@ManyToOne
	@JoinColumn(name = "company_id")
	private Company company;
	@NotBlank
	@Column(columnDefinition = "TEXT")
	private String description;
	@NotBlank
	private String status;
	@Column(length = 300)
	private String location;
	private LocalDate deadline;
	@NotNull
	private LocalDate appliedDate;
	@NotNull
	private boolean saved = false;

	@Column(length = 100)
	private String employmentType;

	@Column(length = 2048)
	private String website;

	public Job() {
	}

	public Job(String title, Company company, String description, String status, String location, LocalDate deadline, LocalDate appliedDate) {
		this.title = title;
		this.company = company;
		this.description = description;
		this.status = status;
		this.location = location;
		this.deadline = deadline;
		this.appliedDate = appliedDate;
	}

	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getExternalId() {
		return this.externalId;
	}

	public void setExternalId(Long externalId) {
		this.externalId = externalId;
	}

	public String getTitle() {
		return this.title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Company getCompany() {
		return this.company;
	}

	public void setCompany(Company company) {
		this.company = company;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getStatus() {
		return this.status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getLocation() {
		return this.location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public LocalDate getDeadline() {
		return this.deadline;
	}
	
	public void setDeadline(LocalDate deadline) {
		this.deadline = deadline;
	}

	public LocalDate getAppliedDate() {
		return this.appliedDate;
	}

	public void setAppliedDate(LocalDate appliedDate) {
		this.appliedDate = appliedDate;
	}

	public Boolean isSaved() {
		return this.saved;
	}

	public void setSaved(Boolean saved) {
		this.saved = saved;
	}

	public String getEmploymentType() {
		return this.employmentType;
	}

	public void setEmploymentType(String employmentType) {
		this.employmentType = employmentType;
	}

	public String getWebsite() {
		return this.website;
	}

	public void setWebsite(String website) {
		this.website = website;
	}
}
