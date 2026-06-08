package com.cvsearch.test;

import com.cvsearch.DTO.JobPatchRequest;
import com.cvsearch.DTO.JobRequest;
import com.cvsearch.DTO.JobResponse;
import com.cvsearch.Job;
import com.cvsearch.JobMapper;
import com.cvsearch.JobNotFoundException;
import com.cvsearch.JobRepository;
import com.cvsearch.JobService;

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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceUnitTest {

    @Mock
    private JobRepository repository;

    @Mock
    private JobMapper jobMapper;

    @InjectMocks
    private JobService jobService;

    private final Job job = new Job("SWE", "Google", "Build stuff", "Applied", LocalDate.of(2026, 6, 1));
    private final JobResponse response = new JobResponse(1L, "SWE", "Google", "Build stuff", "Applied", LocalDate.of(2026, 6, 1));
    private final JobRequest request = new JobRequest("SWE", "Google", "Build stuff", "Applied", LocalDate.of(2026, 6, 1));

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
        assertThat(result.company()).isEqualTo("Google");
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
        when(jobMapper.toEntity(request)).thenReturn(job);
        when(repository.save(job)).thenReturn(job);
        when(jobMapper.toResponse(job)).thenReturn(response);

        JobResponse result = jobService.create(request);

        assertThat(result.title()).isEqualTo("SWE");
        verify(repository).save(job);
    }

    @Test
    void updateJobById_ShouldUpdateAndReturnJob() {
        when(repository.findById(1L)).thenReturn(Optional.of(job));
        when(jobMapper.toEntity(request)).thenReturn(job);
        when(repository.save(job)).thenReturn(job);
        when(jobMapper.toResponse(job)).thenReturn(response);

        JobResponse result = jobService.updateJobById(1L, request);

        assertThat(result.title()).isEqualTo("SWE");
        verify(repository).findById(1L);
        verify(repository).save(job);
    }

    @Test
    void updateJobById_ShouldThrowWhenNotFound() {
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
        when(jobMapper.toResponse(job)).thenReturn(new JobResponse(1L, "Updated Title", "Google", "Build stuff", "Applied", LocalDate.of(2026, 6, 1)));

        JobResponse result = jobService.partialUpdateJobById(1L, patch);

        assertThat(result.title()).isEqualTo("Updated Title");
        verify(jobMapper).applyPartialUpdate(patch, job);
        verify(repository).save(job);
    }

    @Test
    void partialUpdateJobById_ShouldThrowWhenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        JobPatchRequest patch = new JobPatchRequest("Title", null, null, null, null);

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
        String company = "Google";
        String title = "SWE";
        String status = "Applied";

        when(repository.searchJobs(company, title, status)).thenReturn(List.of(job));
        when(jobMapper.toResponseList(any())).thenReturn(List.of(response));

        List<JobResponse> result = jobService.search(company, title, status);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).company()).isEqualTo("Google");
        verify(repository).searchJobs(company, title, status);
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
