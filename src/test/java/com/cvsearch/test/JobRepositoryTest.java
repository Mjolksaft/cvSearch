package com.cvsearch.test;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import com.cvsearch.company.Company;
import com.cvsearch.job.Job;
import com.cvsearch.job.JobRepository;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class JobRepositoryTest {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private TestEntityManager em;

    @Test
    void testFindByCompanyName() {
        Company company = new Company("Google", "https://www.google.com/", "Kristianstad", null);
        em.persist(company);
        em.persist(new Job("SWE", company, "description", "applied", null, null, LocalDate.now()));
        var results = jobRepository.findByCompanyName("Google");
        assertThat(results).hasSize(1);
    }
}
