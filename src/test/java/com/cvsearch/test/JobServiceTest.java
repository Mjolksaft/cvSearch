package com.cvsearch.test;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.cvsearch.company.Company;
import com.cvsearch.job.dto.JobRequest;
import com.cvsearch.job.dto.JobResponse;
import com.cvsearch.company.CompanyRepository;
import com.cvsearch.job.JobService;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
class JobServiceTest {

    @Autowired
    private JobService jobService;

    @Autowired
    private CompanyRepository companyRepository;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void contextLoads() {
    }

    @Test
    void createJob() {
        Company company = companyRepository.save(new Company("Google", "https://www.google.com/", "Kristianstad", null));
        JobRequest request = new JobRequest(
            "Software Engineer",
            company.getId(),
            "Build stuff",
            "Applied",
            LocalDate.of(2026, 6, 1),
            null
        );

        JobResponse response = jobService.create(request);

        assertNotNull(response.id());
        assertEquals("Software Engineer", response.title());
        assertEquals("Google", response.companyName());
    }
}
