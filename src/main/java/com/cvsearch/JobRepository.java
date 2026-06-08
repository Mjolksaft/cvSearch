package com.cvsearch;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByCompany(String company);
    List<Job> findByTitleContainingIgnoreCase(String title);

    @Query(value = """
            SELECT * FROM job j
            WHERE (:company IS NULL OR j.company = :company)
            AND (:title IS NULL OR j.title ILIKE CONCAT('%', :title, '%'))
            AND (:status IS NULL OR j.status = :status)
            """, nativeQuery = true)
    List<Job> searchJobs(@Param("company") String company,
                         @Param("title") String title,
                         @Param("status") String status);
}
