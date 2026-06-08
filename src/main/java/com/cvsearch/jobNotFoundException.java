package com.cvsearch;

public class JobNotFoundException extends RuntimeException {
    public JobNotFoundException(Long id) {
        super("Job not found with id: " + id);
    }
}