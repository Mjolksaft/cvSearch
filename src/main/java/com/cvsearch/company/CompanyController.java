package com.cvsearch.company;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {
    private final CompanyService service;

    public CompanyController(CompanyService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Company> create(@RequestBody Company company) {
        Company created = service.create(company);
        return ResponseEntity.ok(created);
    }

    @GetMapping
    public List<Company> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public Company getById(@PathVariable Long id) {
        return service.getById(id);
    }
}
