package com.cvsearch;

public class jobNotFoundException extends RuntimeException {
    public jobNotFoundException(Long id) {
        super("Job not found with id: " + id);
    }
}