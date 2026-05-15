# 04. Concurrency

- Capacity checks must consider concurrent enrollment attempts.
- A plain `count < capacity` check without locking is not enough for the last-seat race.
- Acceptable strategies:
  - Pessimistic write lock on the class row during enrollment.
  - Optimistic locking with version and retry/failure handling.
  - Capacity slot table or DB constraint strategy if justified.
- Recommended for this assignment:
  - Use pessimistic write lock for clarity and testability.
- Add a concurrency test or document the verification approach.
- The test should prove that concurrent attempts cannot exceed capacity.

