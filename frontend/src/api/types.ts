export type Role = 'HR' | 'JOB_SEEKER' | 'HIRING_MANAGER';

export interface AuthResponse {
  token: string;
  userId: number;
  email: string;
  fullName: string;
  role: Role;
}

export interface JobPosting {
  id: number;
  title: string;
  description: string;
  requiredSkills: string[];
  location: string | null;
  minExperienceYears: number | null;
  status: 'DRAFT' | 'PUBLISHED' | 'CLOSED';
  createdAt: string;
}

export interface CandidateProfile {
  id: number;
  fullName: string;
  headline: string | null;
  skills: string[];
  experienceYears: number | null;
  education: string | null;
  summary: string | null;
  contactEmail: string | null;
  resumeFileName: string | null;
  indexedForMatching: boolean;
}

export interface CandidateMatchView {
  candidateId: number;
  fullName: string;
  headline: string | null;
  skills: string[];
  experienceYears: number | null;
  contactEmail: string | null;
  resumeFileName: string | null;
  score: number;
  rationale: string;
  strengths: string[];
  gaps: string[];
}

export interface TopMatchesResponse {
  jobPostingId: number;
  jobTitle: string;
  topCandidates: CandidateMatchView[];
}
