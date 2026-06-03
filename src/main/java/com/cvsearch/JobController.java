package com.cvsearch;

import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/jobs")
public class JobController {
	private JobService service;

	public JobController(JobService service) {
		this.service = service;
	}

	@GetMapping
	public List<Job> getAll() {
		return service.GetAllJobs();
	}

	@GetMapping("/{id}")
	public Optional<Job> getJobById(@PathVariable Long id) {

		return service.getJobById(id);
	}

	@PostMapping
	public ResponseEntity<Job> create(@RequestBody Job job) {
		Job createdJob = service.create(job);
		return ResponseEntity.ok(createdJob);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteJobById(@PathVariable Long id) {
		service.deleteJobById(id);
		return ResponseEntity.ok().build();
	}

	@PutMapping("/{id}")
	public Job updateJobById(@PathVariable Long id, @Valid @RequestBody Job updatedJob) {
		return service.updateJobById(id, updatedJob);
	}

	@PatchMapping("/{id}")
	public Job partialUpdateJobById(@PathVariable Long id, @Valid @RequestBody Job updatedJob) {
		return service.partialUpdateJobById(id, updatedJob);
	}
}
