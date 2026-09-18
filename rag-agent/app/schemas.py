from typing import List, Optional

from pydantic import BaseModel, Field


class IndexCandidateRequest(BaseModel):
    candidate_id: int
    full_name: str = ""
    headline: str = ""
    skills: List[str] = Field(default_factory=list)
    experience_years: int = 0
    education: str = ""
    summary: str = ""
    resume_text: str = ""


class MatchRequest(BaseModel):
    job_posting_id: int
    title: str
    description: str
    required_skills: List[str] = Field(default_factory=list)
    min_experience_years: Optional[int] = None


class CandidateMatch(BaseModel):
    candidate_id: int = Field(description="The candidate_id exactly as given in the candidate list")
    score: int = Field(description="Overall fit score from 0-100")
    rationale: str = Field(description="2-3 sentences on why this candidate fits (or doesn't)")
    strengths: List[str] = Field(default_factory=list, description="Short phrases naming this candidate's strengths for the role")
    gaps: List[str] = Field(default_factory=list, description="Short phrases naming gaps versus the role's requirements")


class MatchResponse(BaseModel):
    matches: List[CandidateMatch] = Field(default_factory=list)


class RankingOutput(BaseModel):
    """Structured-output target for the ranking LLM call — see graph.py's rank_node."""
    matches: List[CandidateMatch] = Field(
        description="Top candidates for this posting, ordered from best fit to worst fit"
    )
