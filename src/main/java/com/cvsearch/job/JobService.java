package com.cvsearch.job;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.cvsearch.company.Company;
import com.cvsearch.company.CompanyRepository;
import java.util.List;
import java.util.Optional;

import com.cvsearch.job.dto.BulkJobItem;
import com.cvsearch.job.dto.JobPatchRequest;
import com.cvsearch.job.dto.JobRequest;
import com.cvsearch.job.dto.JobResponse;

import jakarta.validation.constraints.NotNull;

@Service
@Validated
public class JobService {
	private final JobRepository repository;
	private final JobMapper jobMapper;
	private final CompanyRepository companyRepository;

	public JobService(JobRepository repository, JobMapper jobMapper, CompanyRepository companyRepository) {
		this.repository = repository;
		this.jobMapper = jobMapper;
		this.companyRepository = companyRepository;
	}

	public Page<JobResponse> GetAllJobs(Pageable pageable) {
		Page<Job> jobs = repository.findAll(pageable);
		return jobs.map(jobMapper::toResponse);
	}

	public JobResponse getJobById(Long id) {
		Job job = repository.findById(id).
			orElseThrow(() -> new JobNotFoundException(id));

		return jobMapper.toResponse(job);
	}

	public JobResponse create(JobRequest request) {
		Job job = jobMapper.toEntity(request);
		Company company = companyRepository.findById(request.companyId())
				.orElseThrow(() -> new RuntimeException("Company not found with id: " + request.companyId()));
		job.setCompany(company);

		Job newJob = repository.save(job);
		return jobMapper.toResponse(newJob);
	}

	public JobResponse updateJobById(@NotNull Long id, @NotNull JobRequest request) {
		
		return repository.findById(id)
			.map(job -> {
				Job newJob = jobMapper.toEntity(request);
				Company company = companyRepository.findById(request.companyId())
						.orElseThrow(() -> new RuntimeException("Company not found with id: " + request.companyId()));
				newJob.setCompany(company);
				newJob = repository.save(newJob);

				return jobMapper.toResponse(newJob);
			})
			.orElseThrow(() -> new JobNotFoundException(id));
	}

	public JobResponse partialUpdateJobById(@NotNull Long id, @NotNull JobPatchRequest request) {
		return repository.findById(id).map(job -> {
			jobMapper.applyPartialUpdate(request, job);
			if (request.companyId() != null) {
				Company company = companyRepository.findById(request.companyId())
						.orElseThrow(() -> new RuntimeException("Company not found with id: " + request.companyId()));
				job.setCompany(company);
			}
			Job updatedJob = repository.save(job);
			return jobMapper.toResponse(updatedJob);
		}).orElseThrow(() -> new JobNotFoundException(id));
	}

	public void deleteJobById(@NotNull Long id) {
		repository.deleteById(id);
	}

	public Page<JobResponse> search(String company, String title, String status,
			String location, Boolean saved, LocalDate appliedBefore, LocalDate appliedAfter,
			Pageable pageable) {
		String likeCompany = company != null ? "%" + company.toLowerCase() + "%" : null;
		String likeTitle = title != null ? "%" + title.toLowerCase() + "%" : null;
		String likeLocation = location != null ? "%" + location.toLowerCase() + "%" : null;
		Page<Job> jobs = repository.searchJobs(likeCompany, likeTitle, status, likeLocation, saved, appliedBefore, appliedAfter, pageable);
		return jobs.map(jobMapper::toResponse);
	}

	public JobResponse updateDescriptionByExternalId(Long externalId, String description) {
		Job job = repository.findByExternalId(externalId)
				.orElseThrow(() -> new JobNotFoundException("Job not found with externalId: " + externalId));
		job.setDescription(description != null && !description.isBlank() ? description : "No description available");
		job = repository.save(job);
		return jobMapper.toResponse(job);
	}

	public List<JobResponse> bulkCreate(List<BulkJobItem> items) {
		return items.stream().map(item -> {
			if (item.externalId() != null) {
				Optional<Job> existing = repository.findByExternalId(item.externalId());
				if (existing.isPresent()) {
					return jobMapper.toResponse(existing.get());
				}
			}

			Company company = companyRepository.findByName(item.companyName())
					.orElseGet(() -> companyRepository.save(
							new Company(item.companyName(), null, null, null)));

			Job job = new Job(
					item.title(),
					company,
					item.description() != null && !item.description().isBlank() ? item.description() : "No description available",
					"Fetched",
					item.location(),
					null,
					LocalDate.now());
			job.setWebsite(item.website());
			job.setExternalId(item.externalId());
			job = repository.save(job);
			return jobMapper.toResponse(job);
		}).toList();
	}
}
