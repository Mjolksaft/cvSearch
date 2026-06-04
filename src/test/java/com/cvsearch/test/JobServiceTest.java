package com.cvsearch.test;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.cvsearch.JobService;
import com.cvsearch.DTO.JobRequest;
import com.cvsearch.DTO.JobResponse;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
@Transactional
class JobServiceTest {

    @Autowired
    private JobService jobService;

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
        JobRequest request = new JobRequest(
            "Software Engineer",
            "Google",
            "Build stuff",
            "Applied",
            LocalDate.of(2026, 6, 1)
        );

        JobResponse response = jobService.create(request);

        assertNotNull(response.id());
        assertEquals("Software Engineer", response.title());
        assertEquals("Google", response.company());
    }
}