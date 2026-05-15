# Lklass Assignment Codex Harness

This repository uses workflow files as top-level orchestration contracts. They are not casual reference docs.

## Mandatory Entry

For any request that mentions or implies a workflow, run the workflow launcher before planning or editing:

```bash
.agents/bin/workflow design
.agents/bin/workflow implement
.agents/bin/workflow test
.agents/bin/workflow review
.agents/bin/workflow inspect
```

Use the closest workflow when the user says things like "기획", "설계", "구현", "개발", "테스트", "리뷰", "점검", "QC", "과제", "README", "API", or "동시성".

After running the launcher:

- Treat the state file printed by the launcher as the current workflow contract.
- Read the workflow file printed by the launcher.
- Use referenced skills as subroutines, not replacements for the workflow.
- Run referenced scripts when instructed.
- Preserve approval gates exactly. Stop and ask when the workflow requires approval.
- Follow the stop conditions printed by the launcher.
- Run `.agents/bin/workflow check` after starting a workflow and before resuming a workflow.
- Run `.agents/bin/workflow guard edit` immediately before file edits during workflow work.
- When the user explicitly approves a gate, record it with the matching command:
  - `.agents/bin/workflow approve plan`
  - `.agents/bin/workflow approve matrix`
  - `.agents/bin/workflow approve slice`
  - `.agents/bin/workflow approve review-fixes`
  - `.agents/bin/workflow approve commit`

## Workflow Map

- `design`: clarify assignment interpretation, API, data model, concurrency policy, tests, README plan, then write `implementation_plan.md`.
- `implement`: read approved `implementation_plan.md`, slice work into `task.md`, implement one vertical slice at a time.
- `test`: propose a test matrix, get approval, then add tests by domain behavior.
- `review`: inspect changed files with assignment requirements, correctness, concurrency, API, README, and test lenses.
- `inspect`: run final QC gates and propose a conventional commit message.

## Assignment Rules

- The selected assignment is `BE-A. 수강 신청 시스템`.
- The required stack is Spring Boot with Java or Kotlin. Prefer Kotlin unless the user changes direction.
- BE submissions require tests. Do not declare completion without meaningful tests.
- README must include every required template section from the assignment.
- Authentication/authorization may be simplified. Use `userId` in a request parameter or header and document the choice.
- Preserve user changes. Do not reset, checkout, clean, or revert unrelated work without explicit approval.
- Prefer small, reviewable commits aligned to working vertical slices.

