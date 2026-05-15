---
description: Final assignment quality gate and commit proposal workflow
---

# Title: 최종 점검 및 커밋 준비

# Description: 제출 전 최종 QC를 수행하고 커밋 메시지를 제안한다.

1. `.agents/skills/lklass-conventions-check/scripts/refresh-rules.sh`를 실행하여 과제 규칙을 읽어라.
2. `git status`로 변경 범위를 확인하라.
3. 가능한 QC gate를 실행하라.
   - `./gradlew test`
   - README 필수 섹션 확인
   - API 목록 및 예시 확인
   - 데이터 모델 설명 확인
   - 실행 방법 확인
   - AI 활용 범위 확인
4. 실패하면 멈추고 `diagnose` 스킬 절차로 원인을 분석하라.
5. 성공하면 변경 요약과 conventional commit 메시지를 제안하라.
6. 유저가 명시적으로 승인하기 전에는 commit/push를 실행하지 마라.

