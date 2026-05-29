package com.cvsearch;

import org.springframework.web.bind.annotation.*;
import java.util.List;

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

    @PostMapping
    public Job create(@RequestBody Job job) {
        return repository.save(job);
    }
}
