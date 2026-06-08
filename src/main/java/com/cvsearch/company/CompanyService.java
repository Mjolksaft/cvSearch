package com.cvsearch.company;

import java.util.List;

import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;

@Service
public class CompanyService {
    private final CompanyRepository repository;

    public CompanyService(CompanyRepository repository) {
        this.repository = repository;
    }

    public Company create(Company company) {
        return repository.save(company);
    }

    public List<Company> getAll() {
        return repository.findAll();
    }

    public Company getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Company not found with id: " + id));
    }
}
