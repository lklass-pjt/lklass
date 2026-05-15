---
description: Assignment code review workflow
---

# Title: 과제 리뷰

# Description: 제출 전 코드, 테스트, README를 요구사항/정확성/동시성/설명력 관점에서 리뷰한다.

1. `.agents/skills/lklass-conventions-check/scripts/refresh-rules.sh`를 실행하여 과제 규칙을 읽어라.
2. `git status`와 `git diff`로 변경 파일 목록을 먼저 파악하라.
3. 리뷰는 아래 순서로 수행하라.
   - 필수 요구사항 누락 여부
   - 상태 전이 오류
   - 정원 초과 및 동시성 취약점
   - 트랜잭션 경계
   - API 요청/응답 일관성
   - 테스트 누락
   - README 필수 항목 누락
   - AI 활용 범위 기재 여부
4. Findings를 심각도 순서로 제시하라.
5. 수정이 필요하면 수정안을 먼저 제안하고, 유저 승인 전에는 review-driven edit을 하지 마라.
6. 수정 승인 후 `.agents/bin/workflow approve review-fixes`를 기록하고 최소 범위로 수정하라.

