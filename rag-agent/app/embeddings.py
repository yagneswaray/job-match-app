"""Local embedding model, wrapped as a LangChain Embeddings implementation.

ONNX MiniLM-L6 (384-dim), bundled via chromadb's embedding_functions utility — we only
use that one utility class, not chromadb's storage/collection API. Wrapping it in
LangChain's Embeddings interface means any LangChain VectorStore (pgvector, OpenSearch,
Pinecone, ...) can use it interchangeably without knowing it's ONNX underneath.
"""
from typing import List

from chromadb.utils.embedding_functions import ONNXMiniLM_L6_V2
from langchain_core.embeddings import Embeddings

EMBEDDING_DIM = 384


class LocalMiniLMEmbeddings(Embeddings):
    """LangChain Embeddings wrapper around chromadb's bundled ONNX MiniLM-L6 model."""

    def __init__(self):
        self._fn = ONNXMiniLM_L6_V2()

    def embed_documents(self, texts: List[str]) -> List[List[float]]:
        return [vector.tolist() for vector in self._fn(texts)]

    def embed_query(self, text: str) -> List[float]:
        return self.embed_documents([text])[0]


_embeddings = None


def get_embeddings() -> LocalMiniLMEmbeddings:
    global _embeddings
    if _embeddings is None:
        _embeddings = LocalMiniLMEmbeddings()
    return _embeddings
