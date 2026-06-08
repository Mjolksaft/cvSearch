package com.cvsearch.job;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.cvsearch.company.Company;
import com.cvsearch.company.CompanyRepository;
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

	public List<JobResponse> search(String company, String title, String status) {
		List<Job> jobs = repository.searchJobs(company, title, status);
		return jobMapper.toResponseList(jobs);
	}
}
