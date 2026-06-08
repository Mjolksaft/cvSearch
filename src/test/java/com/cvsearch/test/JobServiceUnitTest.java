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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
    private final JobResponse response = new JobResponse(1L, "SWE", 1L, "Google", "Build stuff", "Applied", LocalDate.of(2026, 6, 1));

    @Test
    void getAllJobs_ShouldReturnList() {
        when(repository.findAll()).thenReturn(List.of(job));
        when(jobMapper.toResponseList(any())).thenReturn(List.of(response));

        List<JobResponse> result = jobService.GetAllJobs();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("SWE");
        verify(repository).findAll();
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
        JobRequest request = new JobRequest("SWE", 1L, "Build stuff", "Applied", LocalDate.of(2026, 6, 1));
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
        JobRequest request = new JobRequest("SWE", 99L, "Build stuff", "Applied", LocalDate.of(2026, 6, 1));

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
        JobRequest request = new JobRequest("SWE", 1L, "Build stuff", "Applied", LocalDate.of(2026, 6, 1));
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
        JobRequest request = new JobRequest("SWE", 1L, "Build stuff", "Applied", LocalDate.of(2026, 6, 1));

        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.updateJobById(99L, request))
                .isInstanceOf(JobNotFoundException.class)
                .hasMessageContaining("99");

        verify(repository).findById(99L);
        verify(repository, never()).save(any());
    }

    @Test
    void partialUpdateJobById_ShouldApplyPatchAndSave() {
        JobPatchRequest patch = new JobPatchRequest("Updated Title", null, null, null, null);

        when(repository.findById(1L)).thenReturn(Optional.of(job));
        doNothing().when(jobMapper).applyPartialUpdate(patch, job);
        when(repository.save(job)).thenReturn(job);
        when(jobMapper.toResponse(job)).thenReturn(new JobResponse(1L, "Updated Title", 1L, "Google", "Build stuff", "Applied", LocalDate.of(2026, 6, 1)));

        JobResponse result = jobService.partialUpdateJobById(1L, patch);

        assertThat(result.title()).isEqualTo("Updated Title");
        verify(jobMapper).applyPartialUpdate(patch, job);
        verify(repository).save(job);
    }

    @Test
    void partialUpdateJobById_WithCompanyChange_ShouldUpdateCompany() {
        JobPatchRequest patch = new JobPatchRequest(null, 2L, null, null, null);
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
        JobPatchRequest patch = new JobPatchRequest("Title", null, null, null, null);

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

        when(repository.searchJobs(companyName, title, status)).thenReturn(List.of(job));
        when(jobMapper.toResponseList(any())).thenReturn(List.of(response));

        List<JobResponse> result = jobService.search(companyName, title, status);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).companyName()).isEqualTo("Google");
        verify(repository).searchJobs(companyName, title, status);
    }

    @Test
    void search_WithNoFilters_ShouldReturnAll() {
        when(repository.searchJobs(null, null, null)).thenReturn(List.of(job));
        when(jobMapper.toResponseList(any())).thenReturn(List.of(response));

        List<JobResponse> result = jobService.search(null, null, null);

        assertThat(result).hasSize(1);
        verify(repository).searchJobs(null, null, null);
    }
}
