# 01. Scope

- Selected assignment: `BE-A. 수강 신청 시스템`.
- Required:
  - Lecture/Class registration with title, description, price, capacity, start/end dates.
  - Class status: `DRAFT -> OPEN -> CLOSED`.
  - Class list with optional status filter.
  - Class detail with current enrollment count.
  - Enrollment creation.
  - Enrollment status: `PENDING -> CONFIRMED -> CANCELLED`.
  - Payment confirmation as a simple state transition.
  - Enrollment cancellation.
  - My enrollment list.
  - Capacity rule with concurrency consideration.
- Recommended optional scope:
  - Cancellation deadline, such as 7 days after confirmation.
  - Student list by class if time allows.
  - Pagination if implementation cost stays low.
- Avoid implementing waitlist unless the required scope is already excellent.

