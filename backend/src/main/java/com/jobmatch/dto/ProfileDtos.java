package com.jobmatch.dto;

import com.jobmatch.model.CandidateProfile;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public class ProfileDtos {

    public record UpsertRequest(
            @NotBlank String fullName,
            String headline,
            List<String> skills,
            Integer experienceYears,
            String education,
            String summary,
            String contactEmail
    ) {}

    public record Response(
            Long id,
            String fullName,
            String headline,
            List<String> skills,
            Integer experienceYears,
            String education,
            String summary,
            String contactEmail,
            String resumeFileName,
            boolean indexedForMatching
    ) {
        public static Response from(CandidateProfile p) {
            return new Response(
                    p.getId(), p.getFullName(), p.getHeadline(), p.getSkills(),
                    p.getExperienceYears(), p.getEducation(), p.getSummary(),
                    p.getContactEmail(), p.getResumeFileName(), p.isIndexedForMatching());
        }
    }
}
