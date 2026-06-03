package com.cvsearch;

import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.cvsearch.DTO.JobRequest;
import com.cvsearch.DTO.JobResponse;

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
	public ResponseEntity<JobResponse> create(@RequestBody JobRequest job) {
		JobResponse createdJob = service.create(job);
		return ResponseEntity.ok(createdJob);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteJobById(@PathVariable Long id) {
		service.deleteJobById(id);
		return ResponseEntity.ok().build();
	}

	@PutMapping("/{id}")
	public ResponseEntity<JobResponse> updateJobById(@PathVariable Long id, @Valid @RequestBody JobRequest request) {
		JobResponse updatedJob = service.updateJobById(id, request);
		return ResponseEntity.ok(updatedJob);
	}

	@PatchMapping("/{id}")
	public ResponseEntity<JobResponse> partialUpdateJobById(@PathVariable Long id, @Valid @RequestBody JobRequest request) {
		JobResponse updatedJob = service.partialUpdateJobById(id, request);
		return ResponseEntity.ok(updatedJob);
	}
}
