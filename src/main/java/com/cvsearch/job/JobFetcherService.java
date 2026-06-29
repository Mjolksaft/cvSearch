package com.cvsearch.job;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.cvsearch.company.Company;
import com.cvsearch.company.CompanyRepository;
import com.cvsearch.job.dto.JobAd;
import com.cvsearch.job.dto.SearchResponse;

import jakarta.transaction.Transactional;

@Service
public class JobFetcherService {
    private final RestClient restClient;
    private final JobRepository jobRepository;
    private final CompanyRepository companyRepository;

    public JobFetcherService(JobRepository jobRepository,
                             CompanyRepository companyRepository) {
        this.restClient = RestClient.builder()
                .baseUrl("https://jobsearch.api.jobtechdev.se")
                .defaultHeader("Accept", "application/json")
                .build();
        this.jobRepository = jobRepository;
        this.companyRepository = companyRepository;
    }

    private record ApiSearchResponse(ApiTotal total, List<ApiHit> hits) {
        private record ApiTotal(int value) {}
    }

    private record ApiHit(
        String id,
        String headline,
        ApiEmployer employer,
        ApiDescription description,
        ApiWorkplaceAddress workplace_address,
        ApiEmploymentType employment_type,
        ApiOccupation occupation,
        ApiOccupationGroup occupation_group,
        ApiOccupationField occupation_field,
        boolean experience_required,
        String publication_date,
        String application_deadline,
        String webpage_url,
        ApiApplicationDetails application_details
    ) {
        private record ApiEmployer(String name, String organization_number, String workplace) {}
        private record ApiDescription(String text) {}
        private record ApiWorkplaceAddress(String municipality, String region, String city, String country, Double[] coordinates) {}
        private record ApiEmploymentType(String label) {}
        private record ApiOccupation(String label) {}
        private record ApiOccupationGroup(String label) {}
        private record ApiOccupationField(String label) {}
        private record ApiApplicationDetails(String email, String url) {}
    }

    @Transactional
    public SearchResponse searchJobs(String query) {
        ApiSearchResponse apiResponse = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search")
                        .queryParam("q", query)
                        .queryParam("limit", 100)
                        .build())
                .retrieve()
                .body(ApiSearchResponse.class);

        if (apiResponse == null || apiResponse.hits() == null) {
            return new SearchResponse(new SearchResponse.Total(0), List.of());
        }

        List<JobAd> jobAds = apiResponse.hits().stream()
                .map(this::mapToJobAd)
                .toList();

        return new SearchResponse(
                new SearchResponse.Total(apiResponse.total().value()),
                jobAds);
    }

    private JobAd mapToJobAd(ApiHit hit) {
        String employerName = Optional.ofNullable(hit.employer())
                .map(ApiHit.ApiEmployer::name)
                .orElse(null);

        String workplace = Optional.ofNullable(hit.workplace_address())
                .map(ApiHit.ApiWorkplaceAddress::city)
                .orElse(null);

        String description = Optional.ofNullable(hit.description())
                .map(ApiHit.ApiDescription::text)
                .orElse(null);

        String municipality = Optional.ofNullable(hit.workplace_address())
                .map(ApiHit.ApiWorkplaceAddress::municipality)
                .orElse(null);

        String region = Optional.ofNullable(hit.workplace_address())
                .map(ApiHit.ApiWorkplaceAddress::region)
                .orElse(null);

        String employmentType = Optional.ofNullable(hit.employment_type())
                .map(ApiHit.ApiEmploymentType::label)
                .orElse(null);

        String occupation = Optional.ofNullable(hit.occupation())
                .map(ApiHit.ApiOccupation::label)
                .orElse(null);

        String occupationGroup = Optional.ofNullable(hit.occupation_group())
                .map(ApiHit.ApiOccupationGroup::label)
                .orElse(null);

        String occupationField = Optional.ofNullable(hit.occupation_field())
                .map(ApiHit.ApiOccupationField::label)
                .orElse(null);

        LocalDateTime publicationDate = parseDateTime(hit.publication_date());
        LocalDateTime applicationDeadline = parseDateTime(hit.application_deadline());

        String applicationEmail = Optional.ofNullable(hit.application_details())
                .map(ApiHit.ApiApplicationDetails::email)
                .orElse(null);
        String applicationUrl = Optional.ofNullable(hit.application_details())
                .map(ApiHit.ApiApplicationDetails::url)
                .orElse(null);

        saveJobToDatabase(hit);

        return new JobAd(
                hit.id(),
                hit.headline(),
                employerName,
                workplace,
                description,
                municipality,
                region,
                employmentType,
                occupation,
                occupationGroup,
                occupationField,
                hit.experience_required(),
                publicationDate,
                applicationDeadline,
                hit.webpage_url(),
                applicationEmail,
                applicationUrl);
    }

    private void saveJobToDatabase(ApiHit hit) {
        Long externalId = parseLong(hit.id());
        if (externalId == null) {
            return;
        }

        if (jobRepository.findByExternalId(externalId).isPresent()) {
            return; 
        }

        String orgNumberStr = Optional.ofNullable(hit.employer())
                .map(ApiHit.ApiEmployer::organization_number)
                .orElse(null);
        String employerName = Optional.ofNullable(hit.employer())
                .map(ApiHit.ApiEmployer::name)
                .orElse("Unknown Company");

        Company company = null;
        if (orgNumberStr != null) {
            Long orgNumber = parseLong(orgNumberStr);
            if (orgNumber != null) {
                company = companyRepository.findByOrganizationNumber(orgNumber)
                        .orElseGet(() -> {
                            Company newCompany = new Company(employerName, null, null, orgNumber);
                            return companyRepository.save(newCompany);
                        });
            }
        }

        if (company == null) {
            company = companyRepository.findByName(employerName)
                    .orElseGet(() -> companyRepository.save(
                            new Company(employerName, null, null, null)));
        }

        String description = Optional.ofNullable(hit.description())
                .map(ApiHit.ApiDescription::text)
                .orElse("");

        String location = Optional.ofNullable(hit.workplace_address())
                .map(ApiHit.ApiWorkplaceAddress::city)
                .orElse(null);

        java.time.LocalDate deadline = null;
        if (hit.application_deadline() != null) {
            try {
                deadline = java.time.LocalDate.parse(hit.application_deadline().substring(0, 10));
            } catch (Exception e) {
            }
        }

        String employmentType = Optional.ofNullable(hit.employment_type())
                .map(ApiHit.ApiEmploymentType::label)
                .orElse(null);

        Job job = new Job(
                hit.headline(),
                company,
                description,
                "Fetched",
                location,
                deadline,
                java.time.LocalDate.now());
        job.setExternalId(externalId);
        job.setEmploymentType(employmentType);
        job.setWebsite(hit.webpage_url());

        // Extract coordinates from API response (GeoJSON: [longitude, latitude])
        if (hit.workplace_address() != null && hit.workplace_address().coordinates() != null
                && hit.workplace_address().coordinates().length == 2) {
            Double[] coords = hit.workplace_address().coordinates();
            job.setLongitude(coords[0]);
            job.setLatitude(coords[1]);
        }

        jobRepository.save(job);
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.substring(0, 19));
        } catch (Exception e) {
            return null;
        }
    }
}
