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

    @Query(value = """
            SELECT j.* FROM job j
            LEFT JOIN company c ON j.company_id = c.id
            WHERE (:company IS NULL OR c.name ILIKE CONCAT('%', :company, '%'))
            AND (:title IS NULL OR j.title ILIKE CONCAT('%', :title, '%'))
            AND (:status IS NULL OR j.status = :status)
            AND (:location IS NULL OR j.location ILIKE CONCAT('%', :location, '%'))
            AND (:saved IS NULL OR j.saved = :saved)
            AND (:appliedBefore IS NULL OR j.applied_date <= :appliedBefore)
            AND (:appliedAfter IS NULL OR j.applied_date >= :appliedAfter)
            """,
            countQuery = """
            SELECT count(j.*) FROM job j
            LEFT JOIN company c ON j.company_id = c.id
            WHERE (:company IS NULL OR c.name ILIKE CONCAT('%', :company, '%'))
            AND (:title IS NULL OR j.title ILIKE CONCAT('%', :title, '%'))
            AND (:status IS NULL OR j.status = :status)
            AND (:location IS NULL OR j.location ILIKE CONCAT('%', :location, '%'))
            AND (:saved IS NULL OR j.saved = :saved)
            AND (:appliedBefore IS NULL OR j.applied_date <= :appliedBefore)
            AND (:appliedAfter IS NULL OR j.applied_date >= :appliedAfter)
            """,
            nativeQuery = true)
    Page<Job> searchJobs(@Param("company") String company,
                         @Param("title") String title,
                         @Param("status") String status,
                         @Param("location") String location,
                         @Param("saved") Boolean saved,
                         @Param("appliedBefore") LocalDate appliedBefore,
                         @Param("appliedAfter") LocalDate appliedAfter,
                         Pageable pageable);
}
