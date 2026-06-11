package com.cvsearch.userProfile;

import java.util.List;

import org.springframework.stereotype.Service;

import com.cvsearch.user.User;
import com.cvsearch.user.UserRepository;
import com.cvsearch.userProfile.dto.ProfileRequest;
import com.cvsearch.userProfile.dto.ProfileResponse;

import jakarta.persistence.EntityNotFoundException;

@Service
public class UserProfileService {
    private final UserProfileRepository repository;
    private final ProfileMapper profileMapper;
    private final UserRepository userRepository;

    public UserProfileService(UserProfileRepository repository, ProfileMapper profileMapper,
            UserRepository userRepository) {
        this.repository = repository;
        this.profileMapper = profileMapper;
        this.userRepository = userRepository;
    }

    public ProfileResponse getByUserId(Long userId) {
        UserProfile profile = repository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Profile not found for user id: " + userId));
        return profileMapper.toResponse(profile);
    }

    public ProfileResponse createOrReplace(Long userId, ProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        UserProfile profile = repository.findByUserId(userId)
                .orElse(null);

        if (profile == null) {
            profile = profileMapper.toEntity(request);
            profile.setUser(user);
        } else {
            profileMapper.applyUpdate(request, profile);
        }

        UserProfile saved = repository.save(profile);
        return profileMapper.toResponse(saved);
    }

    public List<UserProfile> getAll() {
        return repository.findAll();
    }

    public UserProfile getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Profile not found with id: " + id));
    }
}
