package com.jobmatch.controller;

import com.jobmatch.dto.ProfileDtos.Response;
import com.jobmatch.dto.ProfileDtos.UpsertRequest;
import com.jobmatch.security.AuthenticatedUser;
import com.jobmatch.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @PostMapping
    public Response upsert(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody UpsertRequest request) {
        requireJobSeeker(user);
        return Response.from(profileService.upsert(user.userId(), request));
    }

    @PostMapping(value = "/resume", consumes = "multipart/form-data")
    public Response uploadResume(@AuthenticationPrincipal AuthenticatedUser user, @RequestParam("file") MultipartFile file) {
        requireJobSeeker(user);
        return Response.from(profileService.attachResume(user.userId(), file));
    }

    @GetMapping("/me")
    public Response me(@AuthenticationPrincipal AuthenticatedUser user) {
        requireJobSeeker(user);
        return Response.from(profileService.getByUserId(user.userId()));
    }

    private void requireJobSeeker(AuthenticatedUser user) {
        if (user == null || !"JOB_SEEKER".equals(user.role())) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,
                    "Requires role JOB_SEEKER");
        }
    }
}
