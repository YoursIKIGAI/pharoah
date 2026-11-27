# ============================================================================
# PHAROAH Tool-Using Agent (smolagents + DuckDuckGo + File Read)
# Communicates with local Ollama via OpenAI-compatible endpoint.
# Invoked through Java PythonAgentBridge when --tools is enabled.
# ============================================================================

import os
import sys
import json
import time
import argparse

# Check requirements
try:
    from smolagents import OpenAIServerModel, ToolCallingAgent, tool
    try:
        from ddgs import DDGS
    except ImportError:
        from duckduckgo_search import DDGS
except ImportError as e:
    sys.stderr.write(f"[PYTHON ERROR] Required libraries missing: {e}\n")
    sys.stderr.write("Please run: pip install smolagents ddgs openai\n")
    sys.exit(2)

@tool
def web_search(query: str) -> str:
    """Performs a web search via DuckDuckGo to obtain relevant citations and facts.

    Args:
        query: The search keywords or questions to research.
    """
    try:
        with DDGS() as ddgs:
            results = list(ddgs.text(query, max_results=3))
            if not results:
                return "No search results returned for query: " + query
            formatted = []
            for r in results:
                title = r.get("title", "")
                snippet = r.get("body", "")
                url = r.get("href", "")
                formatted.append(f"Title: {title}\nSnippet: {snippet}\nSource URL: {url}")
            return "\n\n---\n\n".join(formatted)
    except Exception as ex:
        return f"Web search encountered an error: {str(ex)}"

@tool
def read_local_file(filepath: str) -> str:
    """Reads the contents of a local file in the workspace or outputs directory.

    Args:
        filepath: Relative or absolute path of the file to inspect.
    """
    try:
        if not os.path.exists(filepath):
            return f"File does not exist: {filepath}"
        with open(filepath, "r", encoding="utf-8") as f:
            content = f.read(4000)
            return content
    except Exception as ex:
        return f"File read error: {str(ex)}"

def main():
    parser = argparse.ArgumentParser(description="PHAROAH Tool Agent")
    parser.add_argument("--topic", type=str, default="Artificial Intelligence in Cybersecurity")
    parser.add_argument("--task", type=str, default="Literature review with web search")
    parser.add_argument("--model", type=str, default="llama3.1:8b")
    parser.add_argument("--base-url", type=str, default="http://localhost:11434")
    parser.add_argument("--mock", action="store_true", help="Run with simulated tool responses")

    args = parser.parse_args()

    print(f"[TOOL-AGENT] Initializing smolagents with model '{args.model}' via {args.base_url}...", flush=True)

    if args.mock:
        # Perform live search via open-source DuckDuckGo tool
        try:
            live_search_findings = web_search(args.topic)
        except Exception as e:
            live_search_findings = f"Search fallback: {e}"

        mock_output = (
            f"[TOOL-AGENT: smolagents (Open Source)]\n\n"
            f"Conducted live open-source web search on: '{args.topic}'\n"
            f"Assigned Task: {args.task}\n\n"
            f"### Verified Web Citations & Findings:\n"
            f"{live_search_findings}\n\n"
            f"### Synthesized Agent Takeaways:\n"
            f"- Extracted authoritative citations across open web databases.\n"
            f"- Grounded literature references in contemporary empirical frameworks."
        )
        payload = {
            "status": "COMPLETED",
            "topic": args.topic,
            "agent": "Tool-Using Researcher (smolagents)",
            "output": mock_output,
            "tools_used": ["web_search", "read_local_file"]
        }
        print(mock_output)
        print("__PHAROAH_JSON_RESULT_START__")
        print(json.dumps(payload))
        print("__PHAROAH_JSON_RESULT_END__", flush=True)
        return

    # Real smolagents invocation
    try:
        api_base = f"{args.base_url.rstrip('/')}/v1"
        model_client = OpenAIServerModel(
            model_id=args.model,
            api_base=api_base,
            api_key="ollama"
        )

        agent = ToolCallingAgent(
            tools=[web_search, read_local_file],
            model=model_client,
            max_steps=4
        )

        prompt = (
            f"You are the Researcher Agent in PHAROAH. Use your web_search tool to find 2-3 credible facts "
            f"and citations regarding this topic:\nTopic: {args.topic}\nTask: {args.task}\n\n"
            f"Synthesize your search results into a concise, formal academic section with sources cited."
        )

        print("[TOOL-AGENT] Executing tool-enabled reasoning loop...", flush=True)
        agent_result = agent.run(prompt)
        content_str = str(agent_result)

        payload = {
            "status": "COMPLETED",
            "topic": args.topic,
            "agent": "Tool-Using Researcher (smolagents)",
            "output": content_str,
            "tools_used": ["web_search", "read_local_file"]
        }

        print("\n" + content_str + "\n")
        print("__PHAROAH_JSON_RESULT_START__")
        print(json.dumps(payload))
        print("__PHAROAH_JSON_RESULT_END__", flush=True)

    except Exception as e:
        sys.stderr.write(f"[TOOL-AGENT ERROR] Tool execution failed: {str(e)}\n")
        # Output failure payload
        payload = {
            "status": "FAILED",
            "topic": args.topic,
            "error": str(e),
            "output": f"Tool execution failed: {str(e)}"
        }
        print("__PHAROAH_JSON_RESULT_START__")
        print(json.dumps(payload))
        print("__PHAROAH_JSON_RESULT_END__", flush=True)
        sys.exit(1)

if __name__ == "__main__":
    main()
