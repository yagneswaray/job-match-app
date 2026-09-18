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
@Table(name = "job_posting")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobPosting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 4000)
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "job_posting_skill", joinColumns = @JoinColumn(name = "job_posting_id"))
    @Column(name = "skill")
    @Builder.Default
    private List<String> requiredSkills = new ArrayList<>();

    private String location;

    private Integer minExperienceYears;

    /** User id of the HR user who created/published this posting. */
    @Column(nullable = false)
    private Long createdByUserId;

    /** User id of the hiring manager this posting is assigned to. */
    @Column(nullable = false)
    private Long hiringManagerUserId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PostingStatus status = PostingStatus.PUBLISHED;

    @Builder.Default
    private Instant createdAt = Instant.now();

    public enum PostingStatus {
        DRAFT, PUBLISHED, CLOSED
    }
}
