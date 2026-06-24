package com.cvsearch.user;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cvsearch.userProfile.UserProfileRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class UserService {
    private final UserRepository repository;
    private final UserProfileRepository profileRepository;

    public UserService(UserRepository repository, UserProfileRepository profileRepository) {
        this.repository = repository;
        this.profileRepository = profileRepository;
    }

    public User create(User user) {
        return repository.save(user);
    }

    public List<User> getAll() {
        return repository.findAll();
    }

    public User getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
    }

    @Transactional
    public void deleteById(Long id) {
        User user = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
        profileRepository.findByUserId(id).ifPresent(profile -> {
            profile.setUser(null);
            profileRepository.delete(profile);
        });
        repository.delete(user);
    }
}
