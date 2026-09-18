"""Provider-agnostic LLM factory.

graph.py only ever calls `.with_structured_output(...)` on whatever this returns and
chains it into an LCEL pipeline, so swapping LLM_PROVIDER never touches the agent logic
itself. Note: structured output requires tool/function-calling support — Anthropic and
OpenAI models both have it; for Ollama, use a tool-calling-capable model (e.g. llama3.1+).
"""
import os


def get_llm():
    provider = os.getenv("LLM_PROVIDER", "anthropic").lower()

    if provider == "anthropic":
        from langchain_anthropic import ChatAnthropic
        model = os.getenv("ANTHROPIC_MODEL", "claude-sonnet-5")
        return ChatAnthropic(model=model, temperature=0)

    if provider == "openai":
        from langchain_openai import ChatOpenAI
        model = os.getenv("OPENAI_MODEL", "gpt-4o-mini")
        return ChatOpenAI(model=model, temperature=0)

    if provider == "ollama":
        from langchain_community.chat_models import ChatOllama
        model = os.getenv("OLLAMA_MODEL", "llama3")
        base_url = os.getenv("OLLAMA_BASE_URL", "http://localhost:11434")
        return ChatOllama(model=model, base_url=base_url, temperature=0)

    raise ValueError(f"Unsupported LLM_PROVIDER: {provider!r} (expected anthropic | openai | ollama)")
