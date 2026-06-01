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

	@GetMapping
	public Optional<Job> getJobById(Long id) {
		if (id == null) {
			return Optional.empty();
		}
		return repository.findById(id);
	}

	@PostMapping
	public Job create(@RequestBody Job job) {
		if (job == null) {
			return null;
		}
		return repository.save(job);
	}
}
