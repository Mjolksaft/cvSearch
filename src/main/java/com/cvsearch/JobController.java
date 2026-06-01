package com.cvsearch;

import java.util.List;
import java.util.Optional;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/jobs")
public class JobController {
	private final JobRepository repository;

	public JobController(JobRepository repository) {
		this.repository = repository;
	}

	@GetMapping
	public List<Job> getAll() {
		return repository.findAll();
	}

	@GetMapping("/{id}")
	public Optional<Job> getJobById(@PathVariable Long id) {
		if (id == null) {
			return Optional.empty();
		}
		return repository.findById(id);
	}

	@PostMapping
	public Job create(@RequestBody Job job) {
		return repository.save(job);
	}

	@PutMapping("/{id}")
	public Job updateJobById(@PathVariable Long id, @RequestBody Job updatedJob) {
		return repository.findById(id).map(job -> {
			job.setTitle(updatedJob.getTitle());
			job.setCompany(updatedJob.getCompany());
			job.setDescription(updatedJob.getDescription());
			job.setStatus(updatedJob.getStatus());
			job.setAppliedDate(updatedJob.getAppliedDate());
			return repository.save(job);
		}).orElseThrow(() -> new RuntimeException("Job not found with id: " + id));
	}
}
