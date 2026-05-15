---
description: Assignment test planning and implementation workflow
---

# Title: 과제 테스트

# Description: BE-A 요구사항을 검증하는 테스트 매트릭스를 먼저 확정하고 테스트를 작성한다.

1. `.agents/skills/lklass-conventions-check/scripts/refresh-rules.sh`를 실행하여 과제 규칙을 읽어라.
2. `test-planner` 스킬을 활성화하여 테스트 매트릭스를 먼저 작성하라.
3. 테스트 매트릭스에는 최소한 아래 항목을 포함하라.
   - 강의 생성/목록/상세 조회
   - 강의 상태 전이 성공/실패
   - DRAFT/CLOSED 신청 불가, OPEN 신청 가능
   - 정원 초과 신청 거부
   - 마지막 자리 동시 신청 방어
   - PENDING -> CONFIRMED 결제 확정
   - PENDING/CONFIRMED -> CANCELLED 취소
   - 취소 가능 기간 제한을 구현했다면 기간 초과 취소 거부
   - 내 수강 신청 목록 조회
4. 유저 승인 전에는 테스트 파일을 작성하지 마라.
5. 승인 후 논리 조각 단위로 테스트를 작성하고, 각 조각마다 focused test를 실행하라.
6. 최종적으로 `./gradlew test`를 실행하라.

