package com.cvsearch.job;

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
            """, nativeQuery = true)
    List<Job> searchJobs(@Param("company") String company,
                         @Param("title") String title,
                         @Param("status") String status);
}
