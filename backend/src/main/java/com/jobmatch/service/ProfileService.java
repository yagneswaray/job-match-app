package com.jobmatch.service;

import com.jobmatch.dto.ProfileDtos.UpsertRequest;
import com.jobmatch.model.CandidateProfile;
import com.jobmatch.repository.CandidateProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final CandidateProfileRepository candidateProfileRepository;
    private final ResumeParsingService resumeParsingService;
    private final RagAgentClient ragAgentClient;

    public CandidateProfile upsert(Long userId, UpsertRequest request) {
        CandidateProfile profile = candidateProfileRepository.findByUserId(userId)
                .orElseGet(() -> CandidateProfile.builder().userId(userId).build());

        profile.setFullName(request.fullName());
        profile.setHeadline(request.headline());
        profile.setSkills(request.skills() == null ? List.of() : request.skills());
        profile.setExperienceYears(request.experienceYears());
        profile.setEducation(request.education());
        profile.setSummary(request.summary());
        profile.setContactEmail(request.contactEmail());
        profile.setUpdatedAt(Instant.now());

        profile = candidateProfileRepository.save(profile);

        // Index on every save, even before a resume is uploaded — headline/skills/summary
        // alone are enough for retrieval, and indexing again once a resume lands only enriches it.
        ragAgentClient.indexCandidate(profile);
        profile.setIndexedForMatching(true);
        return candidateProfileRepository.save(profile);
    }

    public CandidateProfile attachResume(Long userId, MultipartFile file) {
        CandidateProfile profile = candidateProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Create your profile before uploading a resume"));

        String text = resumeParsingService.extractText(file);
        profile.setResumeFileName(file.getOriginalFilename());
        profile.setResumeText(text);
        profile.setUpdatedAt(Instant.now());
        profile = candidateProfileRepository.save(profile);

        ragAgentClient.indexCandidate(profile);
        profile.setIndexedForMatching(true);
        return candidateProfileRepository.save(profile);
    }

    public CandidateProfile getByUserId(Long userId) {
        return candidateProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
    }
}
