package com.cvsearch;

import java.util.List;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.cvsearch.DTO.JobPatchRequest;
import com.cvsearch.DTO.JobRequest;
import com.cvsearch.DTO.JobResponse;

@Mapper(componentModel = "spring")
public interface JobMapper {
    JobResponse toResponse(Job job);

    Job toEntity(JobRequest request);

    List<JobResponse> toResponseList(List<Job> jobs);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void applyPartialUpdate(JobPatchRequest request, @MappingTarget Job job);
}
