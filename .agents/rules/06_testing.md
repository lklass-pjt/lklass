# 06. Testing

- BE tests are mandatory.
- Required tests:
  - Class creation/list/detail.
  - Class status transition success/failure.
  - Enrollment blocked when class is not open.
  - Enrollment succeeds when class is open and capacity remains.
  - Capacity exceeded error.
  - Concurrent enrollment does not exceed capacity.
  - Payment confirmation transition.
  - Cancellation transition.
  - Cancellation deadline behavior if implemented.
  - My enrollment list.
- Prefer service tests for domain behavior and integration/API tests for request/response contracts.
- Use deterministic time through `Clock` if cancellation deadline is implemented.
- Run `./gradlew test` before final completion.

