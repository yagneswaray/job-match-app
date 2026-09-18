package com.jobmatch.dto;

import java.util.List;

public class MatchDtos {

    /** Request sent from the backend to the Python RAG agent. */
    public record AgentMatchRequest(
            Long jobPostingId,
            String title,
            String description,
            List<String> requiredSkills,
            Integer minExperienceYears
    ) {}

    /** One ranked candidate as returned by the RAG agent (candidateId only — no PII). */
    public record AgentCandidateMatch(
            Long candidateId,
            int score,
            String rationale,
            List<String> strengths,
            List<String> gaps
    ) {}

    public record AgentMatchResponse(
            List<AgentCandidateMatch> matches
    ) {}

    /** Final response returned to the frontend, enriched with candidate contact info. */
    public record CandidateMatchView(
            Long candidateId,
            String fullName,
            String headline,
            List<String> skills,
            Integer experienceYears,
            String contactEmail,
            String resumeFileName,
            int score,
            String rationale,
            List<String> strengths,
            List<String> gaps
    ) {}

    public record TopMatchesResponse(
            Long jobPostingId,
            String jobTitle,
            List<CandidateMatchView> topCandidates
    ) {}
}
