package com.cvsearch.job;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByCompanyName(String name);
    List<Job> findByTitleContainingIgnoreCase(String title);
    Optional<Job> findByExternalId(Long externalId);

    @Query("""
            SELECT j FROM Job j
            LEFT JOIN j.company c
            WHERE (:company IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :company, '%')))
            AND (:title IS NULL OR LOWER(j.title) LIKE LOWER(CONCAT('%', :title, '%')))
            AND (:status IS NULL OR j.status = :status)
            AND (:location IS NULL OR LOWER(j.location) LIKE LOWER(CONCAT('%', :location, '%')))
            AND (:saved IS NULL OR j.saved = :saved)
            AND (:appliedBefore IS NULL OR j.appliedDate <= :appliedBefore)
            AND (:appliedAfter IS NULL OR j.appliedDate >= :appliedAfter)
            """)
    Page<Job> searchJobs(@Param("company") String company,
                         @Param("title") String title,
                         @Param("status") String status,
                         @Param("location") String location,
                         @Param("saved") Boolean saved,
                         @Param("appliedBefore") LocalDate appliedBefore,
                         @Param("appliedAfter") LocalDate appliedAfter,
                         Pageable pageable);
}
