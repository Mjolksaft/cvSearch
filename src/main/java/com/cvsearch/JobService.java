package com.cvsearch;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.cvsearch.DTO.JobPatchRequest;
import com.cvsearch.DTO.JobRequest;
import com.cvsearch.DTO.JobResponse;

import jakarta.validation.constraints.NotNull;

@Service
@Validated
public class JobService {
	private final JobRepository repository;
	private final JobMapper jobMapper;

	public JobService(JobRepository repository, JobMapper jobMapper) {
		this.repository = repository;
		this.jobMapper = jobMapper;
	}

	public List<JobResponse> GetAllJobs() {
		List<Job> jobs = repository.findAll();
		return jobMapper.toResponseList(jobs);
	}

	public JobResponse getJobById(Long id) {
		Job job = repository.findById(id).
			orElseThrow(() -> new JobNotFoundException(id));

		return jobMapper.toResponse(job);
	}

	public JobResponse create(JobRequest request) {
		Job job = jobMapper.toEntity(request);

		Job newJob = repository.save(job);
		return jobMapper.toResponse(newJob);
	}

	public JobResponse updateJobById(@NotNull Long id, @NotNull JobRequest request) {
		
		return repository.findById(id)
			.map(job -> {
				Job newJob = jobMapper.toEntity(request);
				newJob = repository.save(newJob);

				return jobMapper.toResponse(newJob);
			})
			.orElseThrow(() -> new JobNotFoundException(id));
	}

	public JobResponse partialUpdateJobById(@NotNull Long id, @NotNull JobPatchRequest request) {
		return repository.findById(id).map(job -> {
			jobMapper.applyPartialUpdate(request, job);
			Job updatedJob = repository.save(job);
			return jobMapper.toResponse(updatedJob);
		}).orElseThrow(() -> new JobNotFoundException(id));
	}

	public void deleteJobById(@NotNull Long id) {
		repository.deleteById(id);
	}

	public List<JobResponse> search(String company, String title, String status) {
		List<Job> jobs = repository.searchJobs(company, title, status);
		return jobMapper.toResponseList(jobs);
	}
}
