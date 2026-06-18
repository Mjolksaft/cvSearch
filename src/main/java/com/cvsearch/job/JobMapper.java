package com.cvsearch.job;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.cvsearch.job.dto.JobPatchRequest;
import com.cvsearch.job.dto.JobRequest;
import com.cvsearch.job.dto.JobResponse;

@Mapper(componentModel = "spring")
public interface JobMapper {

    @Mapping(target = "companyId", source = "company.id")
    @Mapping(target = "companyName", source = "company.name")
    @Mapping(target = "externalId", source = "externalId")
    JobResponse toResponse(Job job);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "company", ignore = true)
    Job toEntity(JobRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "company", ignore = true)
    void applyPartialUpdate(JobPatchRequest request, @MappingTarget Job job);
}
