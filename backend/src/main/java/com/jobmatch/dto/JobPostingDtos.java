package com.jobmatch.dto;

import com.jobmatch.model.JobPosting;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.time.Instant;
import java.util.List;

public class JobPostingDtos {

    public record CreateRequest(
            @NotBlank String title,
            @NotBlank String description,
            @NotEmpty List<String> requiredSkills,
            String location,
            Integer minExperienceYears,
            @Email @NotBlank String hiringManagerEmail
    ) {}

    public record Response(
            Long id,
            String title,
            String description,
            List<String> requiredSkills,
            String location,
            Integer minExperienceYears,
            JobPosting.PostingStatus status,
            Instant createdAt
    ) {
        public static Response from(JobPosting p) {
            return new Response(
                    p.getId(), p.getTitle(), p.getDescription(), p.getRequiredSkills(),
                    p.getLocation(), p.getMinExperienceYears(), p.getStatus(), p.getCreatedAt());
        }
    }
}
