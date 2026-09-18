package com.jobmatch.controller;

import com.jobmatch.dto.MatchDtos.TopMatchesResponse;
import com.jobmatch.model.JobPosting;
import com.jobmatch.security.AuthenticatedUser;
import com.jobmatch.service.JobPostingService;
import com.jobmatch.service.MatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/postings")
@RequiredArgsConstructor
public class MatchController {

    private final JobPostingService jobPostingService;
    private final MatchService matchService;

    @GetMapping("/{id}/matches")
    public TopMatchesResponse matches(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        if (user == null || !"HIRING_MANAGER".equals(user.role())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Requires role HIRING_MANAGER");
        }
        JobPosting posting = jobPostingService.getOrThrow(id);
        if (!posting.getHiringManagerUserId().equals(user.userId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This posting is not assigned to you");
        }
        return matchService.findTopMatchesForPosting(posting);
    }
}
