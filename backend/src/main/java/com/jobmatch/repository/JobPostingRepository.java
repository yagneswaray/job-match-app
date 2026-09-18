package com.jobmatch.repository;

import com.jobmatch.model.JobPosting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {
    List<JobPosting> findByCreatedByUserId(Long createdByUserId);
    List<JobPosting> findByHiringManagerUserId(Long hiringManagerUserId);
    List<JobPosting> findByStatus(JobPosting.PostingStatus status);
}
