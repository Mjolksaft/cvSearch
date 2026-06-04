package com.cvsearch;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;


public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByCompany(String company);
    List<Job> findByTitleContainingIgnoreCase(String title);
}
