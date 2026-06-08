package com.cvsearch.company;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long>{
    Optional<Company> findByOrganizationNumber(Long organizationNumber);
    Optional<Company> findByName(String name);
}
