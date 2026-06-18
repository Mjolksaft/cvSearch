package com.cvsearch.test;

import com.cvsearch.company.Company;
import com.cvsearch.job.dto.JobPatchRequest;
import com.cvsearch.job.dto.JobRequest;
import com.cvsearch.job.dto.JobResponse;
import com.cvsearch.company.CompanyRepository;
import com.cvsearch.job.JobRepository;
import com.cvsearch.job.JobService;
import com.cvsearch.job.Job;
import com.cvsearch.job.JobMapper;
import com.cvsearch.job.JobNotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceUnitTest {

    @Mock
    private JobRepository repository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private JobMapper jobMapper;

    @InjectMocks
    private JobService jobService;

    private final Company company = new Company("Google", "https://www.google.com/", "Kristianstad", null);
    private final Job job = new Job("SWE", company, "Build stuff", "Applied", null, null, LocalDate.of(2026, 6, 1));
    private final JobResponse response = new JobResponse(1L, "SWE", 1L, "Google", "Build stuff", "Applied", LocalDate.of(2026, 6, 1), false, null, null, null, null, null);

    @Test
    void getAllJobs_ShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Job> jobPage = new PageImpl<>(List.of(job), pageable, 1);

        when(repository.findAll(any(Pageable.class))).thenReturn(jobPage);
        when(jobMapper.toResponse(job)).thenReturn(response);

        Page<JobResponse> result = jobService.GetAllJobs(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).title()).isEqualTo("SWE");
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(repository).findAll(any(Pageable.class));
    }

    @Test
    void getJobById_ShouldReturnJob() {
        when(repository.findById(1L)).thenReturn(Optional.of(job));
        when(jobMapper.toResponse(job)).thenReturn(response);

        JobResponse result = jobService.getJobById(1L);

        assertThat(result.title()).isEqualTo("SWE");
        assertThat(result.companyName()).isEqualTo("Google");
        verify(repository).findById(1L);
    }

    @Test
    void getJobById_ShouldThrowWhenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.getJobById(99L))
                .isInstanceOf(JobNotFoundException.class)
                .hasMessageContaining("99");

        verify(repository).findById(99L);
    }

    @Test
    void create_ShouldSaveAndReturnJob() {
        JobRequest request = new JobRequest("SWE", 1L, "Build stuff", "Applied", LocalDate.of(2026, 6, 1), null, null);
        company.setId(1L);

        when(jobMapper.toEntity(request)).thenReturn(job);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(repository.save(job)).thenReturn(job);
        when(jobMapper.toResponse(job)).thenReturn(response);

        JobResponse result = jobService.create(request);

        assertThat(result.title()).isEqualTo("SWE");
        verify(companyRepository).findById(1L);
        verify(repository).save(job);
    }

    @Test
    void create_ShouldThrowWhenCompanyNotFound() {
        JobRequest request = new JobRequest("SWE", 99L, "Build stuff", "Applied", LocalDate.of(2026, 6, 1), null, null);

        when(jobMapper.toEntity(request)).thenReturn(job);
        when(companyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.create(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");

        verify(companyRepository).findById(99L);
        verify(repository, never()).save(any());
    }

    @Test
    void updateJobById_ShouldUpdateAndReturnJob() {
        JobRequest request = new JobRequest("SWE", 1L, "Build stuff", "Applied", LocalDate.of(2026, 6, 1), null, null);
        company.setId(1L);

        when(repository.findById(1L)).thenReturn(Optional.of(job));
        when(jobMapper.toEntity(request)).thenReturn(job);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(repository.save(job)).thenReturn(job);
        when(jobMapper.toResponse(job)).thenReturn(response);

        JobResponse result = jobService.updateJobById(1L, request);

        assertThat(result.title()).isEqualTo("SWE");
        verify(repository).findById(1L);
        verify(companyRepository).findById(1L);
        verify(repository).save(job);
    }

    @Test
    void updateJobById_ShouldThrowWhenNotFound() {
        JobRequest request = new JobRequest("SWE", 1L, "Build stuff", "Applied", LocalDate.of(2026, 6, 1), null, null);

        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.updateJobById(99L, request))
                .isInstanceOf(JobNotFoundException.class)
                .hasMessageContaining("99");

        verify(repository).findById(99L);
        verify(repository, never()).save(any());
    }

    @Test
    void partialUpdateJobById_ShouldApplyPatchAndSave() {
        JobPatchRequest patch = new JobPatchRequest("Updated Title", null, null, null, null, null, null, null);

        when(repository.findById(1L)).thenReturn(Optional.of(job));
        doNothing().when(jobMapper).applyPartialUpdate(patch, job);
        when(repository.save(job)).thenReturn(job);
        when(jobMapper.toResponse(job)).thenReturn(new JobResponse(1L, "Updated Title", 1L, "Google", "Build stuff", "Applied", LocalDate.of(2026, 6, 1), false, null, null, null, null, null));

        JobResponse result = jobService.partialUpdateJobById(1L, patch);

        assertThat(result.title()).isEqualTo("Updated Title");
        verify(jobMapper).applyPartialUpdate(patch, job);
        verify(repository).save(job);
    }

    @Test
    void partialUpdateJobById_WithCompanyChange_ShouldUpdateCompany() {
        JobPatchRequest patch = new JobPatchRequest(null, 2L, null, null, null, null, null, null);
        Company newCompany = new Company("Meta", "https://meta.com", "Menlo Park", null);
        newCompany.setId(2L);

        when(repository.findById(1L)).thenReturn(Optional.of(job));
        doNothing().when(jobMapper).applyPartialUpdate(patch, job);
        when(companyRepository.findById(2L)).thenReturn(Optional.of(newCompany));
        when(repository.save(job)).thenReturn(job);
        when(jobMapper.toResponse(job)).thenReturn(response);

        JobResponse result = jobService.partialUpdateJobById(1L, patch);

        assertThat(result.companyName()).isEqualTo("Google");
        verify(companyRepository).findById(2L);
        verify(repository).save(job);
    }

    @Test
    void partialUpdateJobById_ShouldThrowWhenNotFound() {
        JobPatchRequest patch = new JobPatchRequest("Title", null, null, null, null, null, null, null);

        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.partialUpdateJobById(99L, patch))
                .isInstanceOf(JobNotFoundException.class)
                .hasMessageContaining("99");

        verify(repository).findById(99L);
        verify(repository, never()).save(any());
    }

    @Test
    void deleteJobById_ShouldCallDelete() {
        jobService.deleteJobById(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void search_ShouldReturnFilteredResults() {
        String companyName = "Google";
        String title = "SWE";
        String status = "Applied";
        Pageable pageable = PageRequest.of(0, 20);
        Page<Job> jobPage = new PageImpl<>(List.of(job), pageable, 1);

        when(repository.searchJobs("%google%", "%swe%", status, null, null, null, null, pageable)).thenReturn(jobPage);
        when(jobMapper.toResponse(job)).thenReturn(response);

        Page<JobResponse> result = jobService.search(companyName, title, status, null, null, null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).companyName()).isEqualTo("Google");
        verify(repository).searchJobs("%google%", "%swe%", status, null, null, null, null, pageable);
    }

    @Test
    void search_WithNoFilters_ShouldReturnAll() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Job> jobPage = new PageImpl<>(List.of(job), pageable, 1);

        when(repository.searchJobs(null, null, null, null, null, null, null, pageable)).thenReturn(jobPage);
        when(jobMapper.toResponse(job)).thenReturn(response);

        Page<JobResponse> result = jobService.search(null, null, null, null, null, null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(repository).searchJobs(null, null, null, null, null, null, null, pageable);
    }
}
