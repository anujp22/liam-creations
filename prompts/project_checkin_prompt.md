Use prompts/codex_task_prompt.md

Analyze this repository and generate a concise project progress report.

Goal
- Summarize the current state of the catalog website project so the report can be pasted into ChatGPT for architecture review and planning.

Instructions
- Do NOT modify code.
- Only analyze the repository.
- Prefer facts from the repo rather than assumptions.
- Keep the report concise, structured, and easy to scan.
- If something is not visible in the repo, say "Not visible" instead of guessing.

Report must cover
- Current repository structure
- Backend package structure
- Implemented features
- Current API endpoints
- Product domain status
- Whether the backend uses mock data or persistence
- Branch/workflow state if visible
- Pending work
- Recommended next 3 tasks
- Potential cleanup/refactor suggestions

Output format

PROJECT CHECK-IN REPORT

1. Project Snapshot
2. Backend Architecture
3. Implemented Features
4. Current API Endpoints
5. Pending Work
6. Recommended Next 3 Tasks
7. Risks / Cleanup Suggestions
