# JobMatch

A job board where HR publishes postings, job seekers build profiles and upload resumes, and hiring
managers get an AI-ranked top-5 shortlist of candidates for each of their postings.

## Architecture

Three independent services:

```
frontend/     React + TypeScript (Vite)         — UI for all three roles
backend/      Spring Boot (Java 21)             — auth, postings, profiles, SQLite
rag-agent/    Python (FastAPI + LangGraph)       — RAG matching agent
```

```
React  ──HTTP──▶  Spring Boot  ──HTTP──▶  RAG agent (LangGraph)
                    (SQLite)                 │
                                              ├─ Postgres + pgvector (Docker, local)
                                              └─ LLM (Anthropic / OpenAI / Ollama — swappable)
```

**Why split it this way:** the backend owns everything transactional (users, postings, profiles,
resume text) in SQLite. It never talks to an LLM directly — it just pushes candidate profiles to the
RAG agent to index, and asks it to rank candidates for a posting. The RAG agent is a separate Python
service because that's where the LangChain/LangGraph/vector-store ecosystem lives, and keeping it
separate means the matching logic (and its LLM dependency) can be swapped, redeployed, or scaled
independently of the transactional backend.

### The matching agent (RAG + LangGraph + LangChain)

`rag-agent/app/graph.py` defines a two-node LangGraph:

```
retrieve  →  rank  →  END
```

Everything around the graph is LangChain-native:

- **`app/embeddings.py`** — `LocalMiniLMEmbeddings`, a LangChain `Embeddings` implementation
  wrapping a local ONNX MiniLM-L6 model (384-dim, no API key, bundled via chromadb's
  `embedding_functions` utility — that's the only thing still borrowed from chromadb).
- **`app/vectorstore.py`** — LangChain's `PGVector` (`langchain_postgres`), backed by the
  Postgres+pgvector container. `graph.py` only ever calls `query_candidates()`/`upsert_candidate()`;
  swapping the backing store (OpenSearch, Pinecone, Milvus, ...) means constructing a different
  LangChain `VectorStore` in this one file — every major vector DB implements the same
  `add_documents` / `similarity_search_with_score` interface, so nothing else changes.
- **`retrieve` node** — calls `vectorstore.similarity_search_with_score()`: fast, cheap vector
  similarity over all indexed candidates.
- **`rank` node** — a LangChain LCEL chain, `ChatPromptTemplate | llm.with_structured_output(RankingOutput)`.
  The LLM reasons about actual fit (skill overlap, seniority, domain) — not just embedding distance —
  and its response is validated straight into a Pydantic model (`app/schemas.py`), no manual JSON
  parsing.
- **`app/llm.py`** — the only place that knows which LLM provider is active. Swappable via
  `rag-agent/.env`'s `LLM_PROVIDER` (`anthropic` | `openai` | `ollama`); everything above it just
  calls `get_llm()` and chains it into LCEL, so provider swaps never touch the graph or prompt.

## Prerequisites

- Java 21, Maven
- Python 3.9+
- Node 18+
- Docker (for Postgres + pgvector)
- An API key for whichever `LLM_PROVIDER` you use in `rag-agent/.env` (or a local Ollama server)

## Running locally

**1. Vector store (Postgres + pgvector)**

```bash
docker compose up -d
```

Starts a free, local `pgvector/pgvector` container on `localhost:5433` (see `docker-compose.yml`).
The RAG agent creates the `vector` extension, table, and HNSW index automatically on first connect.

**2. RAG agent**

```bash
cd rag-agent
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env   # then fill in ANTHROPIC_API_KEY (or switch LLM_PROVIDER)
uvicorn app.main:app --port 8000
```

**3. Backend**

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn spring-boot:run
```

Runs on `:8080`, stores data in `backend/data/jobmatch.db` (SQLite, created automatically).

**4. Frontend**

```bash
cd frontend
npm install
npm run dev
```

Runs on `:5173`.

## Using it

1. Register three accounts at `/register`: one **HR**, one **Hiring manager**, one or more **Job
   seeker**.
2. As HR: publish a posting, assigning it to the hiring manager's email.
3. As a job seeker: fill out the profile form and/or upload a resume (`.pdf`, `.txt`, `.md`) — this
   indexes the candidate into the RAG agent's vector store.
4. As the hiring manager: open the posting and click **Find top 5 candidates**.

## Configuration

**Backend** (`backend/src/main/resources/application.yml`, all overridable via env vars):
`DB_PATH`, `JWT_SECRET`, `JWT_EXPIRATION_MS`, `RAG_AGENT_URL`.

**RAG agent** (`rag-agent/.env`, see `.env.example`):
`LLM_PROVIDER`, `ANTHROPIC_API_KEY`/`ANTHROPIC_MODEL`, `OPENAI_API_KEY`/`OPENAI_MODEL`,
`OLLAMA_MODEL`/`OLLAMA_BASE_URL`, `RETRIEVAL_TOP_K`, `PGVECTOR_URL`.

## Known limitations (MVP)

- JWT secret and dev passwords are for local use only — not production-hardened.
- No password reset, email verification, or account management.
- Resume text is stored as a DB column, not the original file (good enough for matching; no
  original-file download).
- pgvector runs as a single local Docker container — fine for a demo; a real deployment would run
  it as a managed/replicated Postgres instance (e.g. RDS/Aurora with pgvector) instead.
