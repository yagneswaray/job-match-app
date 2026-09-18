"""Vector store for candidate profiles: LangChain's PGVector (Postgres + pgvector).

This is a thin adapter over a LangChain VectorStore. graph.py never sees PGVector or
psycopg directly — it only calls query_candidates()/upsert_candidate() — so swapping
the backing store (OpenSearch, Pinecone, Milvus, ...) means constructing a different
LangChain VectorStore here; every major vector DB implements the same interface
(add_documents / similarity_search_with_score), so nothing else in the app changes.
"""
import os
from threading import Lock
from typing import Any, Dict, List

from langchain_core.documents import Document
from langchain_postgres import PGVector
from langchain_postgres.vectorstores import DistanceStrategy

from .embeddings import get_embeddings

COLLECTION_NAME = "candidates"

_lock = Lock()
_store = None


def _pgvector_url() -> str:
    return os.getenv(
        "PGVECTOR_URL",
        "postgresql+psycopg://jobmatch:jobmatch@localhost:5433/jobmatch_vectors",
    )


def get_vectorstore() -> PGVector:
    global _store
    with _lock:
        if _store is None:
            _store = PGVector(
                embeddings=get_embeddings(),
                collection_name=COLLECTION_NAME,
                connection=_pgvector_url(),
                distance_strategy=DistanceStrategy.COSINE,
                use_jsonb=True,
                create_extension=True,
            )
        return _store


def upsert_candidate(candidate_id: int, document_text: str, metadata: Dict[str, Any]) -> None:
    store = get_vectorstore()
    doc_id = str(candidate_id)
    # add_documents with an id that already exists updates that row in place (verified
    # against this store — no separate delete-then-add dance needed).
    store.add_documents(
        [Document(page_content=document_text or " ", metadata={**metadata, "candidate_id": candidate_id})],
        ids=[doc_id],
    )


def query_candidates(query_text: str, top_k: int = 10) -> List[Dict[str, Any]]:
    store = get_vectorstore()
    results = store.similarity_search_with_score(query_text, k=top_k)
    candidates = []
    for doc, score in results:
        candidates.append({
            "candidate_id": doc.metadata["candidate_id"],
            "document": doc.page_content,
            "metadata": doc.metadata,
            "distance": score,
        })
    return candidates
