package com.cvsearch.userProfile;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.cvsearch.userProfile.dto.ProfileRequest;
import com.cvsearch.userProfile.dto.ProfileResponse;

@RestController
@RequestMapping("/api/profile")
public class UserProfileController {
    private final UserProfileService service;

    public UserProfileController(UserProfileService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ProfileResponse> getProfile(@RequestParam Long userId) {
        ProfileResponse profile = service.getByUserId(userId);
        return ResponseEntity.ok(profile);
    }

    @PutMapping
    public ResponseEntity<ProfileResponse> createOrReplaceProfile(
            @RequestParam Long userId,
            @RequestBody ProfileRequest request) {
        ProfileResponse profile = service.createOrReplace(userId, request);
        return ResponseEntity.ok(profile);
    }
}
