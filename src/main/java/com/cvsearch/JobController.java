package com.cvsearch;

import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
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
	public ResponseEntity<Job> create(@RequestBody Job job) {
		if (job == null || job.getTitle() == null) {
			return ResponseEntity.badRequest().build();
		}
		repository.save(job);
		return ResponseEntity.ok(job);
	}

	@PutMapping("/{id}")
	public Job updateJobById(@PathVariable Long id, @RequestBody Job updatedJob) {
		return repository.findById(id).map(job -> {
			job.setTitle(updatedJob.getTitle());
			job.setCompany(updatedJob.getCompany());
			job.setDescription(updatedJob.getDescription());
			job.setStatus(updatedJob.getStatus());
			return repository.save(job);
		}).orElseThrow(() -> new RuntimeException("Job not found with id: " + id));
	}

	@PatchMapping("/{id}")
	public Job partialUpdateJobById( @PathVariable  Long id, @RequestBody Job updatedJob) {
		return repository.findById(id).map(job -> {

			if (updatedJob.getTitle() != null) job.setTitle(updatedJob.getTitle());
			if (updatedJob.getCompany() != null) job.setCompany(updatedJob.getCompany());
			if (updatedJob.getDescription() != null) job.setDescription(updatedJob.getDescription());
			if (updatedJob.getStatus() != null) job.setStatus(updatedJob.getStatus());
			if (updatedJob.getAppliedDate() != null) job.setAppliedDate(updatedJob.getAppliedDate());
			

			return repository.save(job);
		}).orElseThrow(() -> new RuntimeException("Job not found with id: " + id));
	}


	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteJobById(@PathVariable Long id) {
		if (id == null) { return ResponseEntity.badRequest().build(); }
		if (!repository.existsById(id)) {
			return ResponseEntity.notFound().build();
		}
		repository.deleteById(id);
		return ResponseEntity.noContent().build();
	}
}
