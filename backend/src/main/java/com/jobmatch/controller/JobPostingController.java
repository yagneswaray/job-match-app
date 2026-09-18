package com.jobmatch.controller;

import com.jobmatch.dto.JobPostingDtos.CreateRequest;
import com.jobmatch.dto.JobPostingDtos.Response;
import com.jobmatch.security.AuthenticatedUser;
import com.jobmatch.service.JobPostingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/postings")
@RequiredArgsConstructor
public class JobPostingController {

    private final JobPostingService jobPostingService;

    @PostMapping
    public Response create(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody CreateRequest request) {
        requireRole(user, "HR");
        return Response.from(jobPostingService.create(user.userId(), request));
    }

    @GetMapping("/mine")
    public List<Response> mine(@AuthenticationPrincipal AuthenticatedUser user) {
        requireRole(user, "HR");
        return jobPostingService.findByCreator(user.userId()).stream().map(Response::from).collect(Collectors.toList());
    }

    @GetMapping("/assigned")
    public List<Response> assigned(@AuthenticationPrincipal AuthenticatedUser user) {
        requireRole(user, "HIRING_MANAGER");
        return jobPostingService.findAssignedTo(user.userId()).stream().map(Response::from).collect(Collectors.toList());
    }

    private void requireRole(AuthenticatedUser user, String role) {
        if (user == null || !role.equals(user.role())) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,
                    "Requires role " + role);
        }
    }
}
