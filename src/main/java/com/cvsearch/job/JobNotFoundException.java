package com.cvsearch.job;

public class JobNotFoundException extends RuntimeException {
    public JobNotFoundException(Long id) {
        super("Job not found with id: " + id);
    }

    public JobNotFoundException(String message) {
        super(message);
    }
}
