---
name: test-planner
description: Plan and write tests for BE-A enrollment assignment behavior, API contracts, state transitions, and concurrency.
---

# Test Planner

## Instructions

Create a test matrix before writing tests.

Recommended tests:

- Class registration succeeds.
- Class list supports status filter.
- Class detail includes current enrollment count.
- DRAFT class rejects enrollment.
- OPEN class accepts enrollment.
- CLOSED class rejects enrollment.
- Capacity exceeded enrollment is rejected.
- Concurrent last-seat enrollment does not exceed capacity.
- Payment confirmation changes `PENDING` to `CONFIRMED`.
- Invalid payment confirmation state fails.
- Cancellation changes `PENDING` or `CONFIRMED` to `CANCELLED`.
- Cancellation deadline works if implemented.
- My enrollment list returns only the requested user's enrollments.

Prefer fast service/integration tests that reviewers can run with `./gradlew test`.

