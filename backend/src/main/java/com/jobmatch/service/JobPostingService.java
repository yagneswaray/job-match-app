package com.jobmatch.service;

import com.jobmatch.dto.JobPostingDtos.CreateRequest;
import com.jobmatch.model.JobPosting;
import com.jobmatch.model.Role;
import com.jobmatch.model.User;
import com.jobmatch.repository.JobPostingRepository;
import com.jobmatch.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class JobPostingService {

    private final JobPostingRepository jobPostingRepository;
    private final UserRepository userRepository;

    public JobPosting create(Long hrUserId, CreateRequest request) {
        User hiringManager = userRepository.findByEmail(request.hiringManagerEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No user found with email " + request.hiringManagerEmail()));
        if (hiringManager.getRole() != Role.HIRING_MANAGER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    request.hiringManagerEmail() + " is not registered as a hiring manager");
        }

        JobPosting posting = JobPosting.builder()
                .title(request.title())
                .description(request.description())
                .requiredSkills(request.requiredSkills())
                .location(request.location())
                .minExperienceYears(request.minExperienceYears())
                .createdByUserId(hrUserId)
                .hiringManagerUserId(hiringManager.getId())
                .status(JobPosting.PostingStatus.PUBLISHED)
                .build();
        return jobPostingRepository.save(posting);
    }

    public List<JobPosting> findByCreator(Long hrUserId) {
        return jobPostingRepository.findByCreatedByUserId(hrUserId);
    }

    public List<JobPosting> findAssignedTo(Long hiringManagerUserId) {
        return jobPostingRepository.findByHiringManagerUserId(hiringManagerUserId);
    }

    public JobPosting getOrThrow(Long id) {
        return jobPostingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job posting not found"));
    }
}
