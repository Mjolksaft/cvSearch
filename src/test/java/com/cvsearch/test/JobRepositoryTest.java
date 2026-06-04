package com.cvsearch.test;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import com.cvsearch.Job;
import com.cvsearch.JobRepository;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class JobRepositoryTest {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private TestEntityManager em;

    @Test
    void testFindByCompany() {
        em.persist(new Job("SWE", "Google", "description", "applied", LocalDate.now()));
        var results = jobRepository.findByCompany("Google");
        assertThat(results).hasSize(1);
    }
}
