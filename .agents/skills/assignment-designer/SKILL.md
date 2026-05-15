---
name: assignment-designer
description: Design the BE-A enrollment assignment by clarifying requirements, assumptions, APIs, data model, concurrency, tests, and README deliverables.
---

# Assignment Designer

## Instructions

Build shared understanding in this order:

1. Stack
   - Kotlin or Java.
   - JPA or MyBatis.
   - H2, MySQL, or PostgreSQL.
2. Product interpretation
   - What counts as current enrollment count.
   - Whether `PENDING` occupies capacity.
   - What cancellation deadline means.
3. Domain model
   - Class/Course.
   - Enrollment.
   - User identity simplification.
4. State transitions
   - Class status.
   - Enrollment status.
5. Concurrency
   - Last-seat race.
   - Locking strategy.
6. API
   - Endpoints and request/response examples.
7. Tests
   - Required behavior tests.
   - Concurrency test.
8. README
   - Required sections and explanation strategy.

Ask one question at a time only when the answer matters. Provide a recommended answer with every question.

