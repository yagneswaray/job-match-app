"""LangGraph agent: retrieve candidates via the vector store (RAG), then have the
LLM rank and explain the top fits for a specific job posting.

    retrieve -> rank -> END

`retrieve` is pure vector similarity (fast, cheap, scales to many candidates).
`rank` is where the LLM reasons about actual fit (skills, seniority, domain) beyond
raw embedding distance, and produces the human-readable rationale hiring managers see.
It's built as a LangChain LCEL chain (ChatPromptTemplate | structured-output LLM), so
the LLM's response is validated straight into RankingOutput — no manual JSON parsing.
"""
import os
from typing import Any, Dict, List, TypedDict

from langchain_core.prompts import ChatPromptTemplate
from langgraph.graph import END, StateGraph

from .llm import get_llm
from .schemas import RankingOutput
from .vectorstore import query_candidates

RANK_PROMPT = ChatPromptTemplate.from_messages([
    ("system",
     "You are a technical recruiting assistant helping a hiring manager screen candidates. "
     "Judge actual fit — skill overlap, seniority match, relevant domain background — don't "
     "just copy the vector similarity order the candidates are given in."),
    ("human",
     "Job posting:\n"
     "Title: {title}\n"
     "Description: {description}\n"
     "Required skills: {required_skills}\n"
     "Minimum experience (years): {min_experience_years}\n\n"
     "Candidate profiles below were retrieved from the resume database by semantic similarity "
     "to this posting (lower vector_similarity_distance = closer match):\n\n"
     "{candidate_blocks}\n\n"
     "Select and rank the TOP 5 candidates (fewer only if fewer than 5 were retrieved above) "
     "that best fit THIS posting specifically, ordered from best fit to worst fit."),
])


class MatchState(TypedDict):
    job_posting: Dict[str, Any]
    retrieved: List[Dict[str, Any]]
    ranked: List[Dict[str, Any]]


def retrieve_node(state: MatchState) -> MatchState:
    jp = state["job_posting"]
    query = (
        f"{jp['title']}\n{jp['description']}\n"
        f"Required skills: {', '.join(jp.get('required_skills') or [])}"
    )
    top_k = int(os.getenv("RETRIEVAL_TOP_K", "10"))
    retrieved = query_candidates(query, top_k=top_k)
    return {**state, "retrieved": retrieved}


def _format_candidate_block(c: Dict[str, Any]) -> str:
    meta = c["metadata"]
    return (
        f"candidate_id: {c['candidate_id']}\n"
        f"name: {meta.get('full_name')}\n"
        f"headline: {meta.get('headline')}\n"
        f"skills: {meta.get('skills')}\n"
        f"experience_years: {meta.get('experience_years')}\n"
        f"profile_text: {c['document'][:2000]}\n"
        f"vector_similarity_distance: {c['distance']:.4f}\n"
        "---"
    )


def rank_node(state: MatchState) -> MatchState:
    jp = state["job_posting"]
    retrieved = state["retrieved"]
    if not retrieved:
        return {**state, "ranked": []}

    chain = RANK_PROMPT | get_llm().with_structured_output(RankingOutput)
    result: RankingOutput = chain.invoke({
        "title": jp["title"],
        "description": jp["description"],
        "required_skills": ", ".join(jp.get("required_skills") or []),
        "min_experience_years": jp.get("min_experience_years") or "not specified",
        "candidate_blocks": "\n".join(_format_candidate_block(c) for c in retrieved),
    })
    ranked = [m.model_dump() for m in result.matches]
    return {**state, "ranked": ranked[:5]}


_compiled_graph = None


def get_match_graph():
    global _compiled_graph
    if _compiled_graph is None:
        graph = StateGraph(MatchState)
        graph.add_node("retrieve", retrieve_node)
        graph.add_node("rank", rank_node)
        graph.set_entry_point("retrieve")
        graph.add_edge("retrieve", "rank")
        graph.add_edge("rank", END)
        _compiled_graph = graph.compile()
    return _compiled_graph
