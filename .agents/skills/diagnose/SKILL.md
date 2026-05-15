---
name: diagnose
description: Disciplined diagnosis loop for failing tests, build errors, API bugs, and concurrency issues.
---

# Diagnose

## Instructions

Run `.agents/skills/lklass-conventions-check/scripts/refresh-rules.sh` first.

Use this loop:

1. Reproduce
   - Capture the exact failing command, test, request, response, stack trace, or race symptom.
2. Minimize
   - Reduce to one test, one API call, or one service method.
3. Hypothesize
   - List 3 ranked, falsifiable causes before changing code.
4. Instrument
   - Add targeted temporary logs only when needed. Prefix them with `[DEBUG-...]`.
5. Fix
   - Change the smallest correct surface.
6. Regression test
   - Add or update a test if there is a meaningful seam.
7. Cleanup
   - Remove temporary debug logs and throwaway code.

Do not guess-fix failing tests without a reproducible signal.

