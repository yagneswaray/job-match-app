package com.jobmatch.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.jobmatch.dto.MatchDtos.AgentMatchRequest;
import com.jobmatch.dto.MatchDtos.AgentMatchResponse;
import com.jobmatch.model.CandidateProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * Talks to the Python LangGraph RAG agent service that indexes candidates and ranks them for a posting.
 * The Python side uses Pydantic models with snake_case fields, so JSON in/out of this client is
 * serialized with its own snake_case ObjectMapper — independent of the app's default camelCase mapper
 * used everywhere else (e.g. for the frontend).
 */
@Service
@Slf4j
public class RagAgentClient {

    private final ObjectMapper snakeCaseMapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    @Value("${jobmatch.rag-agent.base-url}")
    private String baseUrl;

    private RestClient client() {
        // Force plain HTTP/1.1 (java.net.HttpURLConnection-based): the JDK HttpClient's default
        // h2c upgrade attempt over http:// confuses uvicorn's HTTP/1.1 parser and drops the body.
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(new org.springframework.http.client.SimpleClientHttpRequestFactory())
                .build();
    }

    /** Push/update a candidate's embedding in the agent's vector store. Best-effort: failures are logged, not fatal. */
    public void indexCandidate(CandidateProfile profile) {
        try {
            Map<String, Object> body = Map.of(
                    "candidate_id", profile.getId(),
                    "full_name", profile.getFullName() == null ? "" : profile.getFullName(),
                    "headline", profile.getHeadline() == null ? "" : profile.getHeadline(),
                    "skills", profile.getSkills() == null ? List.of() : profile.getSkills(),
                    "experience_years", profile.getExperienceYears() == null ? 0 : profile.getExperienceYears(),
                    "education", profile.getEducation() == null ? "" : profile.getEducation(),
                    "summary", profile.getSummary() == null ? "" : profile.getSummary(),
                    "resume_text", profile.getResumeText() == null ? "" : profile.getResumeText()
            );
            client().post()
                    .uri("/index-candidate")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (ResourceAccessException e) {
            log.warn("RAG agent unreachable at {} while indexing candidate {}: {}", baseUrl, profile.getId(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Could not reach the matching agent to index this profile. Is the rag-agent service running on " + baseUrl + "?");
        } catch (HttpStatusCodeException e) {
            log.warn("RAG agent rejected indexing candidate {}: {} {}", profile.getId(), e.getStatusCode(), e.getResponseBodyAsString());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "The matching agent rejected this profile: " + e.getResponseBodyAsString());
        }
    }

    public AgentMatchResponse findTopMatches(AgentMatchRequest request) {
        try {
            String requestJson = snakeCaseMapper.writeValueAsString(request);
            String responseJson = client().post()
                    .uri("/match")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(requestJson)
                    .retrieve()
                    .body(String.class);
            return snakeCaseMapper.readValue(responseJson, AgentMatchResponse.class);
        } catch (ResourceAccessException e) {
            log.error("RAG agent unreachable at {}: {}", baseUrl, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Could not reach the matching agent. Is the rag-agent service running on " + baseUrl + "?");
        } catch (HttpStatusCodeException e) {
            log.error("RAG agent returned an error from {}: {} {}", baseUrl, e.getStatusCode(), e.getResponseBodyAsString());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "The matching agent reached " + baseUrl + " but returned an error: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Unexpected error calling the RAG agent at {}: {}", baseUrl, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Unexpected error calling the matching agent: " + e.getMessage());
        }
    }
}
