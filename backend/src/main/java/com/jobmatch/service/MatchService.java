package com.jobmatch.service;

import com.jobmatch.dto.MatchDtos.AgentCandidateMatch;
import com.jobmatch.dto.MatchDtos.AgentMatchRequest;
import com.jobmatch.dto.MatchDtos.AgentMatchResponse;
import com.jobmatch.dto.MatchDtos.CandidateMatchView;
import com.jobmatch.dto.MatchDtos.TopMatchesResponse;
import com.jobmatch.model.CandidateProfile;
import com.jobmatch.model.JobPosting;
import com.jobmatch.repository.CandidateProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final RagAgentClient ragAgentClient;
    private final CandidateProfileRepository candidateProfileRepository;

    public TopMatchesResponse findTopMatchesForPosting(JobPosting posting) {
        AgentMatchRequest request = new AgentMatchRequest(
                posting.getId(),
                posting.getTitle(),
                posting.getDescription(),
                posting.getRequiredSkills(),
                posting.getMinExperienceYears()
        );

        AgentMatchResponse agentResponse = ragAgentClient.findTopMatches(request);

        List<Long> candidateIds = agentResponse.matches().stream()
                .map(AgentCandidateMatch::candidateId)
                .collect(Collectors.toList());
        Map<Long, CandidateProfile> profilesById = candidateProfileRepository.findAllByIdIn(candidateIds).stream()
                .collect(Collectors.toMap(CandidateProfile::getId, p -> p));

        List<CandidateMatchView> views = agentResponse.matches().stream()
                .map(m -> {
                    CandidateProfile p = profilesById.get(m.candidateId());
                    if (p == null) {
                        return null;
                    }
                    return new CandidateMatchView(
                            p.getId(), p.getFullName(), p.getHeadline(), p.getSkills(),
                            p.getExperienceYears(), p.getContactEmail(), p.getResumeFileName(),
                            m.score(), m.rationale(), m.strengths(), m.gaps());
                })
                .filter(v -> v != null)
                .collect(Collectors.toList());

        return new TopMatchesResponse(posting.getId(), posting.getTitle(), views);
    }
}
