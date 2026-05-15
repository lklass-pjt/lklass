# Lklass 구현 작업 목록

status: in_progress
workflow: implement

## Slice 1. Gradle/Spring Boot 스캐폴딩

- 유형: AFK
- 선행 조건: 없음
- 상태: 완료
- 검증: `./gradlew test` 통과
- 범위:
  - Spring Boot Java 21 스캐폴딩
  - Gradle Kotlin DSL
  - 기본 패키지 `com.lklass`
  - 기본 context load 테스트
  - Testcontainers MySQL 부트스트랩

## Slice 2. 공통 기반

- 유형: AFK
- 선행 조건: Slice 1
- 상태: 완료
- 테스트 워크플로우 검증:
  - `GlobalExceptionHandlerTest`: 비즈니스 예외, 예상 못한 예외, validation 실패 응답 검증
  - `TraceIdFilterTest`: 요청 traceId 사용, traceId 자동 생성, 예외 발생 시 MDC 정리 검증
  - `DomainEventTest`: 이벤트 타입과 payload 보존 검증
  - 검증 명령: `./gradlew test --tests 'com.lklass.global.*' --tests com.lklass.LklassApplicationTests` 통과
- 실행/검증 가능 항목:
  - 공통 응답 테스트
  - 에러 코드/예외 핸들러 테스트
  - traceId 필터 테스트
  - Clock/configuration properties context 테스트
- 범위:
  - `CommonResponse`
  - `ErrorCode`
  - `BusinessException`
  - `GlobalExceptionHandler`
  - `TraceIdFilter`
  - `ClockConfig`
  - 기본 configuration properties
  - 기본 이벤트 모델
  - logback 패턴

## Slice 3. 설정과 DB 기반

- 유형: AFK
- 선행 조건: Slice 1, Slice 2
- 상태: 완료
- 검증:
  - `./gradlew test --tests com.lklass.global.config.DatabaseFoundationTest` 통과
  - `./gradlew test` 통과
- 테스트 워크플로우 검증:
  - `DatabaseFoundationTest`: Flyway V1 migration의 `shedlock` 테이블 생성 검증
  - `DatabaseFoundationTest`: ShedLock `LockProvider` bean 등록 검증
  - `DatabaseFoundationTest`: `JwtProperties`, `EnrollmentPolicyProperties`, `SchedulerProperties` yml 바인딩 검증
- 실행/검증 가능 항목:
  - Flyway migration이 Testcontainers MySQL에 적용됨
  - JPA schema validate 통과
  - ShedLock 테이블 존재 검증
- 범위:
  - 기본 yml 설정
  - Flyway V1 migration
  - MySQL/Testcontainers 이미지 버전 고정
  - ShedLock dependency/config
  - 기본 audit/time column은 실제 엔티티가 추가되는 Slice 4/5에서 함께 구현

## Slice 4. User/Auth/Security/JWT

- 유형: AFK
- 선행 조건: Slice 2, Slice 3
- 상태: 진행 중
- 진행 내역:
  - Slice 4-A 완료: User 엔티티, UserRole, users Flyway migration, UserJpaRepository 추가
  - `UserPersistenceTest`: User 저장/조회와 email unique 제약 검증
  - Slice 4-B 완료: 회원가입 서비스, UserRepository wrapper, PasswordEncoder 추가
  - `AuthServiceTest`: 비밀번호 암호화 저장, userId 반환, 중복 email 예외 검증
  - 테스트 워크플로우 보강: User 필수 필드 null 방어와 BCrypt PasswordEncoder 검증 추가
  - Slice 4-C 완료: JWT access token 발급/검증, 로그인 서비스, 회원가입 accessToken 반환 적용
  - `JwtTokenProviderTest`: token claim 파싱, 만료 token, 잘못된 token 검증
  - `AuthServiceTest`: 회원가입 token 반환, 로그인 성공/실패 검증
  - Slice 4-D 완료: AuthController, 요청 DTO validation 메시지, AuthSecurityConfigurer, SecurityConfig 추가
- 검증:
  - `./gradlew test --tests com.lklass.domain.user.entity.UserPersistenceTest` 통과
  - `./gradlew test --tests com.lklass.domain.auth.service.AuthServiceTest` 통과
  - `./gradlew test --tests com.lklass.global.security.JwtTokenProviderTest` 통과
  - `./gradlew test --tests com.lklass.domain.auth.controller.AuthControllerTest` 통과
  - `./gradlew test` 통과
- 실행/검증 가능 항목:
  - 회원가입 성공
  - 로그인 시 access token 반환
  - 인증이 필요한 endpoint가 미인증 요청을 거부
  - role claim이 Authentication에 반영됨
- 범위:
  - User entity/repository/service/controller
  - password encoding
  - JWT provider/filter
  - security config
  - auth DTO

## Slice 5. Course 생성, 조회, 상태 전이

- 유형: AFK
- 선행 조건: Slice 4
- 상태: 대기
- 실행/검증 가능 항목:
  - creator가 Course 생성
  - Course 목록/상세 조회
  - `DRAFT -> OPEN` 성공
  - `OPEN -> CLOSED` 성공
  - 잘못된 상태 전이 실패
  - Course 상태 이력이 같은 트랜잭션에서 기록됨
- 범위:
  - Course entity
  - CourseStatusHistory
  - Course repository wrapper/JPA repository
  - Course service/controller/DTO
  - pagination response

## Slice 6. Course 스케줄러

- 유형: AFK
- 선행 조건: Slice 5
- 상태: 대기
- 실행/검증 가능 항목:
  - 예약된 Course 자동 OPEN
  - 모집 마감 Course 자동 CLOSED
  - ShedLock으로 중복 job 실행 방지
  - API가 모집 기간을 방어적으로 재검증
- 범위:
  - 게시/모집 예약
  - Course 자동 상태 전환 스케줄러
  - scheduler properties
  - Clock 기반 테스트

## Slice 7. 수강 신청과 정원 확보

- 유형: HITL
- 선행 조건: Slice 5
- 상태: 대기
- 실행/검증 가능 항목:
  - OPEN Course의 모집 기간 내 수강 신청 성공
  - DRAFT/CLOSED/모집 전/모집 마감 Course 신청 거부
  - 정원 초과 신청 거부
  - 동일 사용자의 활성 중복 신청 차단
  - `occupiedCount`와 `ActiveEnrollment` 정합성 유지
- 범위:
  - Enrollment entity
  - ActiveEnrollment entity
  - EnrollmentStatusHistory
  - Course repository의 atomic conditional update
  - Enrollment service/controller/DTO

## Slice 8. 결제 확정과 취소

- 유형: AFK
- 선행 조건: Slice 7
- 상태: 대기
- 실행/검증 가능 항목:
  - `PENDING -> CONFIRMED` 성공
  - 잘못된 결제 확정 실패
  - PENDING 취소 성공
  - CONFIRMED 7일 이내 취소 성공
  - CONFIRMED 7일 초과 취소 실패
  - 취소 시 좌석 release와 ActiveEnrollment 삭제
- 범위:
  - 결제 확정 use case
  - 취소 use case
  - cancel reason
  - Clock 기반 취소 기한 테스트

## Slice 9. PENDING 신청 만료

- 유형: AFK
- 선행 조건: Slice 8
- 상태: 대기
- 실행/검증 가능 항목:
  - 30분 초과 PENDING이 CANCELLED로 변경
  - 만료된 PENDING은 결제 확정 불가
  - 만료 시 좌석 release와 ActiveEnrollment 삭제
  - 만료 후 다른 사용자가 신청 가능
- 범위:
  - PENDING 만료 스케줄러
  - ShedLock
  - 만료 대상 조회 repository query
  - 상태 이력

## Slice 10. 목록, 수강생 조회, 페이지네이션

- 유형: AFK
- 선행 조건: Slice 8
- 상태: 대기
- 실행/검증 가능 항목:
  - 내 수강 신청 목록 페이지 조회
  - Course 수강생 목록 페이지 조회
  - creator는 본인 Course 수강생 조회 가능
  - admin은 모든 Course 수강생 조회 가능
  - student는 수강생 목록 조회 불가
- 범위:
  - query repository methods
  - page response
  - enrollment list DTO
  - course students DTO

## Slice 11. 동시성 테스트와 README 마무리

- 유형: HITL
- 선행 조건: Slice 7, Slice 10
- 상태: 대기
- 실행/검증 가능 항목:
  - 정원 5명 Course에 100건 virtual-thread 신청 시도 시 정원 초과 없음
  - 최종 `occupiedCount`와 활성 신청 수 일치
  - 전체 `./gradlew test` 통과
  - README에 과제 필수 섹션 모두 포함
- 범위:
  - 동시성 통합 테스트
  - README
  - API 예시
  - ERD/데이터 모델 설명
  - AI 활용 내역
