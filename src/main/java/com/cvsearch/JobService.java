package com.cvsearch;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.cvsearch.DTO.JobRequest;
import com.cvsearch.DTO.JobResponse;

import jakarta.validation.constraints.NotNull;

@Service
@Validated
public class JobService {
	@NotNull
	private final JobRepository repository;

	public JobService(JobRepository repository) {
		this.repository = repository;
	}

	public List<Job> GetAllJobs() { // correct
		return repository.findAll();
	}

	public Optional<Job> getJobById(@NotNull Long id) { // correct
		return repository.findById(id);
	}

	public JobResponse create(JobRequest request) {
		Job job = new Job(request.title(), request.company(), request.description(), request.status(),
				request.appliedDate());
		Job newJob = repository.save(job);
		return new JobResponse(newJob.getTitle(), newJob.getCompany(), newJob.getDescription(), newJob.getStatus());
	}

	public JobResponse updateJobById(@NotNull Long id, @NotNull JobRequest request) {
		return repository.findById(id)
			.map(job -> {
				job.setTitle(request.title());
				job.setCompany(request.company());
				job.setDescription(request.description());
				job.setStatus(request.status());
				job.setAppliedDate(request.appliedDate());

				Job updatedJob = repository.save(job);

				return new JobResponse(updatedJob.getTitle(), updatedJob.getCompany(), updatedJob.getDescription(), updatedJob.getStatus());
			})
			.orElseThrow(() -> new JobNotFoundException(id));
	}

	public JobResponse partialUpdateJobById(@NotNull Long id, @NotNull JobRequest request) {
		return repository.findById(id).map(job -> {
			if (request.title() != null)
				job.setTitle(request.title());
			if (request.company() != null)
				job.setCompany(request.company());
			if (request.description() != null)
				job.setDescription(request.description());
			if (request.status() != null)
				job.setStatus(request.status());
			if (request.appliedDate() != null)
				job.setAppliedDate(request.appliedDate());
			Job updatedJob = repository.save(job);
			return new JobResponse(updatedJob.getTitle(), updatedJob.getCompany(), updatedJob.getDescription(), updatedJob.getStatus());
		}).orElseThrow(() -> new JobNotFoundException(id));
	}

	public void deleteJobById(@NotNull Long id) {
		repository.deleteById(id);
	}
}
