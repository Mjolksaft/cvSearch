package com.cvsearch;

import java.util.List;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.cvsearch.DTO.JobPatchRequest;
import com.cvsearch.DTO.JobRequest;
import com.cvsearch.DTO.JobResponse;

@Mapper(componentModel = "spring")
public interface JobMapper {
    JobResponse toResponse(Job job);

    @Mapping(target = "id", ignore = true)
    Job toEntity(JobRequest request);

    List<JobResponse> toResponseList(List<Job> jobs);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    void applyPartialUpdate(JobPatchRequest request, @MappingTarget Job job);
}
