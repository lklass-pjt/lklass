---
name: lklass-conventions-check
description: Validate Lklass assignment changes against BE-A requirements, Spring architecture, concurrency, tests, README, and AI usage rules.
---

# Lklass Conventions Check

## Use this skill when

- Creating or editing Spring code.
- Creating or editing tests.
- Creating or editing README or assignment documentation.
- Reviewing before a commit or submission.

## Instructions

### Step 0: Rule refresh

Run `scripts/refresh-rules.sh` before checking code.

### Checklist

#### Assignment completeness

- [ ] Required BE-A features are implemented or explicitly planned.
- [ ] Optional features are either implemented cleanly or documented as skipped.
- [ ] README required sections exist.

#### Domain behavior

- [ ] Class status transitions are explicit and invalid transitions fail.
- [ ] Enrollment status transitions are explicit and invalid transitions fail.
- [ ] Capacity policy is documented and enforced.
- [ ] Monetary values avoid floating point.

#### Concurrency

- [ ] Last-seat concurrent enrollment cannot exceed capacity.
- [ ] Locking or constraint strategy is understandable and tested/documented.

#### Spring structure

- [ ] Controller does not contain business rules.
- [ ] State-changing use cases are transactional.
- [ ] Repositories are not called directly from controllers.
- [ ] API DTOs do not expose entities directly.

#### Tests

- [ ] Required behavior tests exist.
- [ ] Concurrency behavior has a test or a clear verification note.
- [ ] `./gradlew test` passes before declaring completion.

#### README

- [ ] 실행 방법 is enough for a reviewer to run the project.
- [ ] API examples are copyable.
- [ ] 데이터 모델 설명 is clear.
- [ ] AI 활용 범위 is honest and concise.

