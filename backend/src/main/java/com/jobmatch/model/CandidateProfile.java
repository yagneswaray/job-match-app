package com.jobmatch.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "candidate_profile")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The job-seeker user this profile belongs to (one-to-one). */
    @Column(nullable = false, unique = true)
    private Long userId;

    private String fullName;

    private String headline;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "candidate_skill", joinColumns = @JoinColumn(name = "candidate_profile_id"))
    @Column(name = "skill")
    @Builder.Default
    private List<String> skills = new ArrayList<>();

    private Integer experienceYears;

    private String education;

    @Column(length = 2000)
    private String summary;

    private String contactEmail;

    private String resumeFileName;

    @Lob
    @Column(length = 20000)
    private String resumeText;

    @Builder.Default
    private boolean indexedForMatching = false;

    @Builder.Default
    private Instant updatedAt = Instant.now();
}
