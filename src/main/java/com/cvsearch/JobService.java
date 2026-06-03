package com.cvsearch;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
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

	public Job create(@NotNull Job job) {
		return repository.save(job);
	}

	public Job updateJobById(@NotNull Long id, @NotNull Job updatedJob) {
		return repository.findById(id).map(job -> {
			job.setTitle(updatedJob.getTitle());
			job.setCompany(updatedJob.getCompany());
			job.setDescription(updatedJob.getDescription());
			job.setStatus(updatedJob.getStatus());
			return repository.save(job);
		}).orElseThrow(() -> new jobNotFoundException(id));
	}

	public Job partialUpdateJobById(@NotNull Long id, @NotNull Job updatedJob) {
		return repository.findById(id).map(job -> {

			if (updatedJob.getTitle() != null) job.setTitle(updatedJob.getTitle());
			if (updatedJob.getCompany() != null) job.setCompany(updatedJob.getCompany());
			if (updatedJob.getDescription() != null) job.setDescription(updatedJob.getDescription());
			if (updatedJob.getStatus() != null) job.setStatus(updatedJob.getStatus());
			if (updatedJob.getAppliedDate() != null) job.setAppliedDate(updatedJob.getAppliedDate());
			

			return repository.save(job);
		}).orElseThrow(() -> new jobNotFoundException(id));
	}

	public void deleteJobById(@NotNull Long id) {
		repository.deleteById(id);
	}
}
