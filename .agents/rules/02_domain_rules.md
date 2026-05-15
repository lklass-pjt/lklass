# 02. Domain Rules

- A class in `DRAFT` cannot accept enrollments.
- A class in `OPEN` can accept enrollments.
- A class in `CLOSED` cannot accept enrollments.
- Allowed class transition:
  - `DRAFT -> OPEN`
  - `OPEN -> CLOSED`
- Disallowed class transition should fail explicitly.
- Enrollment state transitions:
  - `PENDING -> CONFIRMED`
  - `PENDING -> CANCELLED`
  - `CONFIRMED -> CANCELLED`
- Disallowed enrollment transition should fail explicitly.
- Define and document the capacity policy:
  - Recommended: `PENDING + CONFIRMED` occupy capacity, `CANCELLED` does not.
- If cancellation deadline is implemented, define the clock basis and test it.
- Monetary values should not use floating point types.

