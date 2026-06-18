package com.cvsearch.userProfile;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.cvsearch.userProfile.dto.ProfileRequest;
import com.cvsearch.userProfile.dto.ProfileResponse;

@Mapper(componentModel = "spring")
public interface ProfileMapper {

    @Mapping(target = "userId", source = "user.id")
    ProfileResponse toResponse(UserProfile profile);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    UserProfile toEntity(ProfileRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    void applyUpdate(ProfileRequest request, @MappingTarget UserProfile profile);
}
