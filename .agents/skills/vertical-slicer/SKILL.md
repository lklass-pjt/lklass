---
name: vertical-slicer
description: Break the approved BE-A assignment plan into small implementable vertical slices.
---

# Vertical Slicer

## Instructions

Run `.agents/skills/lklass-conventions-check/scripts/refresh-rules.sh` first.

Create slices that are independently verifiable. Prefer slices like:

- Project skeleton and baseline test.
- Class creation/list/detail.
- Class state transitions.
- Enrollment creation with capacity policy.
- Pessimistic lock and concurrency test.
- Payment confirmation and cancellation.
- My enrollment list.
- README/API examples/final polish.

For each slice, show:

- Title
- Type: HITL or AFK
- Blocked by
- What becomes runnable or testable
- Files likely touched

Ask for approval before writing `task.md`.

