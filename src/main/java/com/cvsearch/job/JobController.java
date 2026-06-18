package com.cvsearch.job;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.cvsearch.job.dto.BulkJobItem;
import com.cvsearch.job.dto.DescriptionUpdateRequest;
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

	public JobController(JobService service,
						 JobFetcherService fetcherService) {
		this.service = service;
		this.fetcherService = fetcherService;
	}

	@GetMapping
	public Page<JobResponse> getAll(
			@RequestParam(required = false) String company,
			@RequestParam(required = false) String title,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String location,
			@RequestParam(required = false) Boolean saved,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate appliedBefore,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate appliedAfter,
			@PageableDefault(sort = "appliedDate", direction = Sort.Direction.DESC) Pageable pageable) {

		if (company != null || title != null || status != null
				|| location != null || saved != null
				|| appliedBefore != null || appliedAfter != null) {
			return service.search(company, title, status, location, saved, appliedBefore, appliedAfter, pageable);
		}
		return service.GetAllJobs(pageable);
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

	@PostMapping("/bulk")
	public ResponseEntity<List<JobResponse>> bulkCreate(@RequestBody @Valid List<BulkJobItem> items) {
		List<JobResponse> created = service.bulkCreate(items);
		return ResponseEntity.ok(created);
	}

	@PatchMapping("/external/{externalId}")
	public ResponseEntity<JobResponse> updateDescriptionByExternalId(
			@PathVariable Long externalId,
			@RequestBody DescriptionUpdateRequest request) {
		JobResponse updated = service.updateDescriptionByExternalId(externalId, request.description());
		return ResponseEntity.ok(updated);
	}

	@GetMapping("/fetch")
	public ResponseEntity<SearchResponse> fetchJobs(@RequestParam(defaultValue = "java") String q) {
		SearchResponse response = fetcherService.searchJobs(q);
		return ResponseEntity.ok(response);
	}

}
