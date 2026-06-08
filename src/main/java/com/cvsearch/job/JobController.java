package com.cvsearch.job;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.cvsearch.job.dto.JobAd;
import com.cvsearch.job.dto.JobPatchRequest;
import com.cvsearch.job.dto.JobRequest;
import com.cvsearch.job.dto.JobResponse;
import com.cvsearch.job.dto.SearchResponse;

import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/jobs")
public class JobController {
	private JobService service;
	private JobFetcherService fetcherService;

	public JobController(JobService service, JobFetcherService fetcherService) {
		this.service = service;
		this.fetcherService = fetcherService;
	}

	@GetMapping
	public List<JobResponse> getAll(
			@RequestParam(required = false) String company,
			@RequestParam(required = false) String title,
			@RequestParam(required = false) String status) {

		if (company != null || title != null || status != null) {
			return service.search(company, title, status);
		}
		return service.GetAllJobs();
	}

	@GetMapping("/{id}")
	public JobResponse getJobById(@PathVariable Long id) {

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
	public ResponseEntity<JobResponse> partialUpdateJobById(@PathVariable Long id, @Valid @RequestBody JobPatchRequest request) {
		JobResponse updatedJob = service.partialUpdateJobById(id, request);
		return ResponseEntity.ok(updatedJob);
	}

	@GetMapping("/fetch")
	public ResponseEntity<SearchResponse> fetchJobs(@RequestParam(defaultValue = "java") String q) {
		SearchResponse response = fetcherService.searchJobs(q);
		return ResponseEntity.ok(response);
	};
}
