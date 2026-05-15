---
description: Assignment design and approval workflow
---

# Title: 과제 설계

# Description: BE-A 수강 신청 시스템 구현 전 요구사항 해석, API, 데이터 모델, 동시성, 테스트, README 계획을 확정한다.

1. `.agents/skills/lklass-conventions-check/scripts/refresh-rules.sh`를 실행하여 과제 규칙을 읽어라.
2. `assignment-designer` 스킬을 활성화하여 유저와 설계를 맞춰라.
   - 한 번에 하나씩 질문하라.
   - 질문과 함께 추천안을 제시하라.
   - 이미 요구사항에서 명확한 것은 질문하지 말고 합리적으로 확정하라.
3. 설계에서 반드시 확정할 것:
   - 기술 스택: Kotlin/Java, JPA/MyBatis, H2/MySQL/PostgreSQL
   - 인증 단순화 방식: header 또는 parameter의 `userId`
   - 강의 상태 전이 정책
   - 수강 신청 상태 전이 정책
   - 정원 점유 기준: `PENDING + CONFIRMED` 또는 `CONFIRMED` only
   - 동시성 제어 방식: pessimistic lock, optimistic lock, or DB constraint strategy
   - 취소 가능 기간 구현 여부
   - 대기열/수강생 목록/페이지네이션 구현 여부
   - README 필수 항목 작성 계획
   - 테스트 매트릭스
4. 인터뷰가 끝나면 `implementation_plan.md`를 작성하라.
5. 설계안에는 아래 항목을 포함하라.
   - 프로젝트 목표와 비목표
   - 요구사항 해석 및 가정
   - 도메인 모델
   - 상태 전이 표
   - API 목록과 요청/응답 예시
   - 동시성 제어 설계
   - 예외/에러 응답 정책
   - 테스트 계획
   - README 제출 항목 계획
6. 명시적 승인 전에는 프로젝트 코드 구현으로 넘어가지 마라.
7. 승인을 받으면 `.agents/bin/workflow approve plan`을 기록하고, "설계가 승인되었습니다. `/implement` 워크플로우로 구현을 시작할 수 있습니다."라고 안내하라.

