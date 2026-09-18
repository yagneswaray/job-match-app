from dotenv import load_dotenv

load_dotenv()

import logging

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware

from .graph import get_match_graph
from .schemas import CandidateMatch, IndexCandidateRequest, MatchRequest, MatchResponse
from .vectorstore import upsert_candidate

logger = logging.getLogger("jobmatch.rag_agent")

app = FastAPI(title="Job Match RAG Agent", version="0.1.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/index-candidate")
def index_candidate(req: IndexCandidateRequest):
    document = "\n".join(filter(None, [
        req.headline,
        ("Skills: " + ", ".join(req.skills)) if req.skills else "",
        f"Experience: {req.experience_years} years" if req.experience_years else "",
        req.education,
        req.summary,
        req.resume_text,
    ]))
    metadata = {
        "full_name": req.full_name,
        "headline": req.headline,
        "skills": ", ".join(req.skills),
        "experience_years": req.experience_years,
    }
    upsert_candidate(req.candidate_id, document, metadata)
    return {"status": "indexed", "candidate_id": req.candidate_id}


@app.post("/match", response_model=MatchResponse)
def match(req: MatchRequest):
    graph = get_match_graph()
    try:
        result = graph.invoke({
            "job_posting": {
                "title": req.title,
                "description": req.description,
                "required_skills": req.required_skills,
                "min_experience_years": req.min_experience_years,
            },
            "retrieved": [],
            "ranked": [],
        })
    except Exception as e:
        logger.exception("Match graph failed for posting %s", req.job_posting_id)
        raise HTTPException(status_code=502, detail=f"{type(e).__name__}: {e}") from e

    matches = [CandidateMatch(**m) for m in result.get("ranked", [])]
    return MatchResponse(matches=matches)
