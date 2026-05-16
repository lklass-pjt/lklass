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
- 상태: 완료
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
  - Slice 4-E1 완료: JwtAuthenticationFilter, AuthenticatedUser, SecurityContext 인증 저장 추가
  - Slice 4-E2 완료: AuthenticationEntryPoint, AccessDeniedHandler 공통 실패 응답 추가
  - 테스트 워크플로우 보강: 잘못된 Bearer Access Token의 MVC 공통 실패 응답 검증 추가
- 검증:
  - `./gradlew test --tests com.lklass.domain.user.entity.UserPersistenceTest` 통과
  - `./gradlew test --tests com.lklass.domain.auth.service.AuthServiceTest` 통과
  - `./gradlew test --tests com.lklass.global.security.JwtTokenProviderTest` 통과
  - `./gradlew test --tests com.lklass.domain.auth.controller.AuthControllerTest` 통과
  - `./gradlew test --tests com.lklass.global.security.JwtAuthenticationFilterTest` 통과
  - `./gradlew test --tests com.lklass.global.common.CommonResponseTest --tests com.lklass.domain.auth.controller.AuthControllerTest --tests com.lklass.global.security.JwtAuthenticationFilterTest` 통과
  - `./gradlew test --tests com.lklass.global.security.JwtAuthenticationFilterTest --tests com.lklass.domain.auth.controller.AuthControllerTest` 통과
  - `./gradlew test` 통과
  - `./gradlew check` 통과
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
- 상태: 완료
- 전체 목표:
  - creator가 Course 생성
  - Course 목록 조회와 상태 필터
  - Course 상세 조회와 현재 신청 인원 포함
  - `DRAFT -> OPEN` 성공
  - `OPEN -> CLOSED` 성공
  - 잘못된 상태 전이 실패
  - Course 상태 이력이 같은 트랜잭션에서 기록됨

### Slice 5-A. Course 엔티티와 스키마

- 상태: 완료
- 완료 내역:
  - Course 엔티티, CourseStatus, CourseStatusHistory, courses Flyway migration 추가
  - CoursePrice, CourseCapacity, CoursePeriod, CourseStatusChangedBy 값 객체 적용
  - CourseStatusChangeReason enum 적용
  - CourseStatusHistory는 Course 단방향 ManyToOne으로 상태 이력의 소유 관계를 표현
  - 리뷰 보강: `occupied_count <= capacity` DB CHECK 제약 추가
  - 리뷰 보강: CoursePrice 금액을 소수점 2자리로 정규화하고 2자리 초과 입력 거부
  - 리뷰 보강: Course 제목/설명 공백 값 도메인 레벨 거부
- 현재 검증 가능 항목:
  - Course 생성 시 기본 상태 `DRAFT`
  - Course 생성 시 `occupiedCount = 0`
  - 가격, 정원, 모집 기간, 수강 기간 생성 규칙
  - CourseStatusHistory 기본 이력 값 보존
  - Flyway migration과 JPA entity 매핑 validate
- 검증:
  - `./gradlew test --tests com.lklass.domain.course.entity.CourseEntityTest` 통과
  - `./gradlew test --tests com.lklass.LklassApplicationTests` 통과
  - `./gradlew test` 통과
  - `./gradlew check` 통과

### Slice 5-B. Course 생성 API

- 상태: 완료
- 목표:
  - creator/admin이 Course 생성
  - student는 Course 생성 불가
  - 생성된 Course는 `DRAFT` 상태
  - 생성 시 CREATED 상태 이력이 같은 트랜잭션에서 저장됨
- 진행 내역:
  - Slice 5-B1 완료: CourseJpaRepository, CourseRepository wrapper, CourseStatusHistory repository 추가
  - Slice 5-B1 완료: CourseService 생성 use case 추가
  - Slice 5-B1 완료: Course 생성 시 CREATED 상태 이력 저장 검증
  - Slice 5-B1 보정: CourseService가 엔티티 대신 CourseCreateResult를 반환하도록 변경
  - Slice 5-B2 완료: CourseController, Course 생성 request/response DTO 추가
  - Slice 5-B2 완료: `@PreAuthorize` 기반 Course 생성 권한 검증 추가
  - Slice 5-B2 완료: CREATOR는 본인 명의 생성, ADMIN은 creatorId 지정 생성 정책 적용
  - Slice 5-B2 완료: 서비스 method security 거부 예외가 403 공통 응답으로 내려가도록 보강
  - Slice 5-B2 보강: ADMIN 대리 생성 시 상태 이력의 changedBy를 실제 행위자인 ADMIN으로 기록
  - 테스트 워크플로우 보강: Course 생성 테스트를 API 계약, 권한 정책, 서비스 통합 테스트로 분리
  - 테스트 워크플로우 보강: CoursePermissionTest에서 CREATOR/ADMIN/STUDENT 권한 조합 전체 검증
  - 테스트 워크플로우 보강: CourseControllerTest는 정상 생성, 401, 대표 403, validation 400 API 계약만 검증
  - 테스트 워크플로우 보강: CourseServiceTest는 저장 성공, creator 존재/role 검증, `@PreAuthorize` 대표 차단 검증으로 정리
- 검증:
  - `./gradlew test --tests com.lklass.domain.course.security.CoursePermissionTest` 통과
  - `./gradlew test --tests com.lklass.domain.course.controller.CourseControllerTest` 통과
  - `./gradlew test --tests com.lklass.domain.course.service.CourseServiceTest` 통과
  - `./gradlew test --tests com.lklass.domain.course.entity.CourseEntityTest --tests com.lklass.domain.course.service.CourseServiceTest` 통과
  - `./gradlew test --tests com.lklass.domain.course.entity.CourseEntityTest --tests com.lklass.domain.course.service.CourseServiceTest --tests com.lklass.domain.course.controller.CourseControllerTest` 통과
  - `./gradlew test` 통과
  - `./gradlew check` 통과
- 범위:
  - CourseJpaRepository
  - CourseRepository wrapper
  - CourseService
  - CourseController
  - Course 생성 request/response DTO
  - CourseSecurityConfigurer 또는 method security 권한 검증

### Slice 5-C. Course 목록/상세 조회

- 상태: 완료
- 목표:
  - Course 목록 조회
  - status 필터 조회
  - Course 상세 조회
  - 상세 응답에 현재 신청 인원 포함
- 진행 내역:
  - Slice 5-C1 완료: Course 목록/상세 조회 service use case 추가
  - Slice 5-C1 완료: Course 목록 조회에 Pageable과 status 필터 적용
  - Slice 5-C1 완료: Course 상세 조회 결과에 현재 신청 인원 `occupiedCount` 포함
  - Slice 5-C1 완료: 없는 Course 상세 조회 시 `COURSE_NOT_FOUND` 예외 처리
  - Slice 5-C2 완료: Course 목록/상세 조회 API 추가
  - Slice 5-C2 완료: Course 조회 응답에 `creatorName` 포함
  - Slice 5-C2 완료: Course 조회를 User join DTO projection으로 변경
  - Slice 5-C2 완료: 공통 페이지 응답과 1-base pageable 정책 추가
  - 테스트 워크플로우 보강: 잘못된 status query parameter가 400 validation 공통 응답으로 반환되는지 검증
  - 테스트 워크플로우 보강: query parameter 타입 변환 실패를 `VALIDATION_ERROR`로 변환하는 공통 예외 처리 검증
- 검증:
  - `./gradlew test --tests com.lklass.domain.course.service.CourseServiceTest` 통과
  - `./gradlew test --tests com.lklass.domain.course.controller.CourseControllerTest` 통과
  - `./gradlew test --tests com.lklass.global.exception.GlobalExceptionHandlerTest` 통과
  - `./gradlew test --tests 'com.lklass.domain.course.*' --tests com.lklass.global.exception.GlobalExceptionHandlerTest` 통과
- 범위:
  - Course 목록/상세 DTO
  - CourseRepository 조회 메서드
  - CourseService 조회 use case
  - CourseController 조회 API
  - pagination은 복잡도가 낮으면 함께 적용

### Slice 5-D. Course 상태 전이와 이력

- 상태: 완료
- 목표:
  - `DRAFT -> OPEN` 성공
  - `OPEN -> CLOSED` 성공
  - `DRAFT -> CLOSED`, `CLOSED -> OPEN` 같은 잘못된 전이 실패
  - 상태 변경 이력이 같은 트랜잭션에서 저장됨
- 진행 내역:
  - Slice 5-D1 완료: Course.open(), Course.close() 도메인 상태 전이 메서드 추가
  - Slice 5-D1 완료: Course 수동 OPEN/CLOSED service use case 추가
  - Slice 5-D1 완료: 잘못된 상태 전이 시 `INVALID_COURSE_STATUS_TRANSITION` 예외 처리
  - Slice 5-D1 완료: 수동 상태 변경 이력을 같은 트랜잭션에서 저장
  - Slice 5-D1 완료: ADMIN은 모든 Course, CREATOR는 본인 Course만 상태 변경 가능하도록 검증
  - Slice 5-D1 보강: Course 상태 변경 권한을 `@PreAuthorize`와 CoursePermission으로 통일
  - Slice 5-D1 보강: CREATOR 소유 Course 확인은 `existsByIdAndCreatorId`로 가볍게 검증
  - Slice 5-D1 보강: 수동 OPEN은 요청한 모집 마감일을 받고 모집 시작일을 현재 시각으로 변경
  - Slice 5-D1 보강: 수동 OPEN 모집 마감일이 현재 시각 이후가 아니면 `ENROLLMENT_CLOSED` 예외 처리
  - Slice 5-D1 보강: 수동 OPEN 모집 마감일이 수강 시작일 이전이 아니면 `INVALID_ENROLLMENT_PERIOD` 예외 처리
  - 테스트 워크플로우 보강: Course entity 상태 전이 성공/실패 규칙을 단위 테스트로 고정
  - 테스트 워크플로우 보강: Course 관리 권한의 인증 없음/principal 타입 불일치 경계 테스트 추가
  - Slice 5-D2 완료: Course 수동 OPEN/CLOSE API 추가
  - Slice 5-D2 완료: OPEN API는 요청 body로 모집 마감일을 받도록 구현
  - Slice 5-D2 완료: 상태 전이 API의 성공, 401, 403, 404, 잘못된 상태 전이, validation 실패 계약 검증
  - 테스트 워크플로우 보강: 상태 변경 API 404 계약은 ADMIN 기준으로 검증하고 CREATOR의 없는 Course 변경은 403으로 검증
- 검증:
  - `./gradlew test --tests com.lklass.domain.course.entity.CourseEntityTest` 통과
  - `./gradlew test --tests com.lklass.domain.course.security.CoursePermissionTest` 통과
  - `./gradlew test --tests com.lklass.domain.course.service.CourseServiceTest` 통과
  - `./gradlew test --tests com.lklass.domain.course.controller.CourseControllerTest` 통과
  - `./gradlew test --tests com.lklass.domain.course.entity.CourseEntityTest --tests com.lklass.domain.course.service.CourseServiceTest` 통과
  - `./gradlew test --tests 'com.lklass.domain.course.*'` 통과
- 범위:
  - Course.open()
  - Course.close()
  - CourseStatusHistory 저장 repository
  - CourseService 상태 전이 use case
  - CourseController 상태 전이 API

## Slice 6. Course 스케줄러

- 유형: AFK
- 선행 조건: Slice 5
- 상태: 진행 중
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

### Slice 6-A. Course 게시/모집 예약

- 상태: 완료
- 목표:
  - creator/admin이 DRAFT Course를 자동 OPEN 대상에 포함하도록 예약
  - DRAFT가 아닌 Course에는 자동 게시 예약 불가
  - 모집 마감이 이미 지난 Course에는 자동 게시 예약 불가
- 진행 내역:
  - Course 자동 게시 예약 도메인 메서드 추가
  - Course 게시 예약 service/API 추가
  - Course 게시 예약 권한은 기존 Course 관리 권한과 동일하게 적용
  - 리뷰 보강: 자동 게시 예약도 모집 마감일이 수강 시작일 이전인지 검증
  - 테스트 워크플로우 보강: 게시 예약 service/API 성공, 권한, unknown Course, 상태/기간 실패 계약 검증
- 검증:
  - `./gradlew test --tests com.lklass.domain.course.entity.CourseEntityTest` 통과
  - `./gradlew test --tests com.lklass.domain.course.service.CourseServiceTest` 통과
  - `./gradlew test --tests com.lklass.domain.course.controller.CourseControllerTest` 통과
  - `./gradlew test --tests 'com.lklass.domain.course.*'` 통과
  - `./gradlew test` 통과

### Slice 6-B1. 예약된 Course 자동 OPEN 유스케이스

- 상태: 완료
- 목표:
  - 예약 게시된 DRAFT Course가 모집 시작 시간이 되면 자동 OPEN
  - 자동 OPEN 시 AUTO_OPENED 상태 이력을 SYSTEM 변경 주체로 저장
  - 모집 시작 전/모집 마감 후 Course는 자동 OPEN 대상에서 제외
- 진행 내역:
  - Course 자동 OPEN 도메인 메서드 추가
  - 자동 OPEN 대상 조회 repository query 추가
  - CourseService에 스케줄러가 호출할 openReservedCourses use case 추가
  - 수동 OPEN 성공 시 자동 게시 예약 플래그를 해제하도록 보강
  - 테스트 워크플로우 보강: 자동 OPEN 시간 경계, 미예약 제외, 복수 대상 처리 검증
- 검증:
  - `./gradlew test --tests com.lklass.domain.course.entity.CourseEntityTest` 통과
  - `./gradlew test --tests com.lklass.domain.course.service.CourseServiceTest` 통과
  - `./gradlew test --tests 'com.lklass.domain.course.*'` 통과
  - `./gradlew test` 통과

### Slice 6-B2. Course 자동 OPEN 스케줄러 실행기

- 상태: 완료
- 목표:
  - 예약된 Course 자동 OPEN 유스케이스를 cron 기반으로 주기 실행
  - ShedLock으로 다중 인스턴스 중복 실행 방지
  - 자동 OPEN 대상 조회를 위한 복합 인덱스 추가
- 진행 내역:
  - CourseStatusScheduler 추가
  - Spring scheduling 활성화
  - 자동 OPEN 대상 조회 최적화 인덱스 추가
- 검증:
  - `./gradlew test --tests com.lklass.domain.course.scheduler.CourseStatusSchedulerTest` 통과
  - `./gradlew test --tests 'com.lklass.domain.course.*'` 통과
  - `./gradlew test` 통과

### Slice 6-C1. 모집 마감 Course 자동 CLOSED 유스케이스

- 상태: 완료
- 목표:
  - 모집 마감 시간이 지난 OPEN Course를 자동 CLOSED
  - 자동 CLOSED 시 AUTO_CLOSED 상태 이력을 SYSTEM 변경 주체로 저장
  - 아직 모집 중인 OPEN Course는 자동 CLOSED 대상에서 제외
- 진행 내역:
  - Course 자동 CLOSED 도메인 메서드 추가
  - 자동 CLOSED 대상 조회 repository query 추가
  - CourseService에 스케줄러가 호출할 closeExpiredOpenCourses use case 추가
  - 테스트 워크플로우 보강: 자동 CLOSED 복수 대상 처리와 OPEN이 아닌 만료 Course 제외 검증
- 검증:
  - `./gradlew test --tests com.lklass.domain.course.entity.CourseEntityTest` 통과
  - `./gradlew test --tests com.lklass.domain.course.service.CourseServiceTest` 통과
  - `./gradlew test --tests com.lklass.global.config.DatabaseFoundationTest` 통과
  - `./gradlew test --tests 'com.lklass.domain.course.*'` 통과
  - `./gradlew test` 통과

### Slice 6-C2. Course 상태 동기화 스케줄러 연결

- 상태: 완료
- 목표:
  - 같은 cron job에서 예약 Course 자동 OPEN과 모집 마감 Course 자동 CLOSED를 함께 실행
  - 자동 CLOSED 대상 조회를 위한 복합 인덱스 추가
- 진행 내역:
  - CourseStatusScheduler가 openReservedCourses와 closeExpiredOpenCourses를 함께 호출하도록 변경
  - 자동 CLOSED 조회 최적화 인덱스 추가
  - 테스트 워크플로우 보강: 상태 동기화 호출 순서, scheduler bean 조건, 자동 상태 동기화 인덱스 생성 검증
- 검증:
  - `./gradlew test --tests com.lklass.domain.course.scheduler.CourseStatusSchedulerTest` 통과
  - `./gradlew test --tests com.lklass.global.config.DatabaseFoundationTest` 통과
  - `./gradlew test --tests 'com.lklass.domain.course.*'` 통과
  - `./gradlew test` 통과

## Slice 7. 수강 신청과 정원 확보

- 유형: HITL
- 선행 조건: Slice 5
- 상태: 진행 중
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

### Slice 7-A. Enrollment 엔티티와 스키마

- 상태: 완료
- 목표:
  - 수강 신청 상태 `PENDING -> CONFIRMED -> CANCELLED`를 저장할 기본 모델 준비
  - MySQL에서 partial unique index 대신 `active_enrollments` 테이블로 활성 신청 중복 방지 준비
  - 수강 신청 상태 이력을 append-only로 저장할 테이블 준비
- 완료 내역:
  - Enrollment, ActiveEnrollment, EnrollmentStatusHistory 엔티티 추가
  - EnrollmentStatus, EnrollmentStatusChangeReason, EnrollmentStatusChangedBy 추가
  - enrollments, active_enrollments, enrollment_status_histories Flyway migration 추가
  - ActiveEnrollment `(course_id, user_id)` unique 제약으로 같은 Course/User의 활성 신청 중복 방지
- 현재 검증 가능 항목:
  - Enrollment 저장 시 기본 상태 `PENDING`
  - ActiveEnrollment unique 제약으로 중복 활성 신청 거부
  - EnrollmentStatusHistory 변경 사유와 변경 주체 저장
  - ActiveEnrollment unique 제약 범위와 Enrollment 단일 점유 제약 검증
  - EnrollmentStatusHistory SYSTEM 변경 주체 저장 검증
  - 수강 신청 기본 테이블 Flyway 생성 검증
- 검증:
  - `./gradlew test --tests com.lklass.domain.enrollment.entity.EnrollmentPersistenceTest` 통과
  - `./gradlew test --tests 'com.lklass.domain.enrollment.*'` 통과
  - `./gradlew test` 통과

### Slice 7-B. Course 정원 원자성 update

- 상태: 완료
- 목표:
  - 수강 신청 트랜잭션에서 사용할 Course 좌석 확보/반납 연산 준비
  - 정원 초과 여부를 단일 조건부 update 결과로 판단할 수 있게 함
- 완료 내역:
  - CourseJpaRepository에 `tryOccupySeat`, `releaseSeat` 조건부 원자성 update 추가
  - CourseRepository wrapper에 boolean 반환 메서드 추가
  - OPEN/모집 기간/정원 조건과 좌석 반납 동작을 Testcontainers MySQL로 검증
- 현재 검증 가능 항목:
  - 정원이 남은 OPEN Course 좌석 확보 성공
  - 정원이 찬 Course 좌석 확보 실패
  - DRAFT 또는 모집 마감 Course 좌석 확보 실패
  - 좌석 반납 성공 및 0 미만 감소 방지
  - 모집 시작 전/CLOSED/없는 Course 좌석 확보 실패
  - 좌석 확보 누적 증가 검증
- 검증:
  - `./gradlew test --tests com.lklass.domain.course.repository.CourseCapacityRepositoryTest` 통과
  - `./gradlew test --tests 'com.lklass.domain.course.repository.*'` 통과
  - `./gradlew test` 통과

### Slice 7-C. 수강 신청 서비스 use case

- 상태: 완료
- 목표:
  - STUDENT가 OPEN Course에 수강 신청하면 좌석을 확보하고 PENDING 신청을 생성
  - 활성 중복 신청과 정원 초과를 명확한 비즈니스 예외로 변환
  - 수강 신청 생성 이력을 같은 트랜잭션에서 저장
- 완료 내역:
  - Enrollment/ActiveEnrollment/EnrollmentStatusHistory repository 추가
  - EnrollmentRepository wrapper에서 ActiveEnrollment unique 충돌을 `ALREADY_ENROLLED`로 변환
  - EnrollmentService 신청 use case 추가
  - EnrollmentApplyResult DTO 추가
  - EnrollmentErrorCode 추가
- 현재 검증 가능 항목:
  - STUDENT 신청 성공 시 `occupiedCount` 증가, Enrollment/ActiveEnrollment/History 저장
  - STUDENT가 아닌 사용자의 신청 권한 거부
  - 같은 Course/User 활성 중복 신청 거부
  - 정원 초과 신청 거부
  - DRAFT Course 신청 거부
  - CLOSED/모집 전/모집 마감 Course 신청 거부
  - 존재하지 않는 Course 신청 거부
  - 존재하지 않는 사용자 신청 거부
  - 같은 Course에 서로 다른 STUDENT 신청 시 `occupiedCount` 누적 증가
- 검증:
  - 테스트 워크플로우 보강: 수강 신청 서비스의 상태/기간/사용자 존재성/복수 학생 경계 테스트 추가
  - `./gradlew test --tests com.lklass.domain.enrollment.service.EnrollmentServiceTest` 통과
  - `./gradlew test --tests 'com.lklass.domain.enrollment.*'` 통과
  - `./gradlew test` 통과

### Slice 7-D. 수강 신청 API

- 상태: 완료
- 목표:
  - 수강 신청 service use case를 HTTP API로 노출
  - 수강 신청 성공/실패 API 응답 계약 고정
- 완료 내역:
  - `POST /api/courses/{courseId}/enrollments` API 추가
  - EnrollmentApplyResponse DTO 추가
  - EnrollmentController WebMvcTest 추가
- 현재 검증 가능 항목:
  - 인증된 STUDENT 수강 신청 성공 응답
  - 미인증 요청 401 응답
  - 권한 없는 요청 403 응답
  - 활성 중복 신청 409 응답
  - 정원 초과 409 응답
  - 신청 불가 Course 400 응답
  - 존재하지 않는 Course 404 응답
  - 존재하지 않는 사용자 404 응답
  - 잘못된 `courseId` path variable 400 validation 응답
- 검증:
  - 테스트 워크플로우 보강: 사용자 없음 404와 잘못된 `courseId` 400 API 계약 추가
  - `./gradlew test --tests com.lklass.domain.enrollment.controller.EnrollmentControllerTest` 통과
  - `./gradlew test --tests 'com.lklass.domain.enrollment.*'` 통과
  - `./gradlew test` 통과

## Slice 8. 결제 확정과 취소

- 유형: AFK
- 선행 조건: Slice 7
- 상태: 완료
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

### Slice 8-A. 결제 확정 use case

- 상태: 완료
- 목표:
  - 외부 결제 연동 없이 신청 상태를 `PENDING -> CONFIRMED`로 확정
  - 결제 확정 시각과 상태 이력을 저장
- 완료 내역:
  - Enrollment `confirm` 도메인 메서드 추가
  - EnrollmentService `confirmPayment` use case 추가
  - 결제 확정 요청자를 `AuthenticatedUser`로 받고 신청 소유자 검증 추가
  - 결제 확정 이력의 changedBy를 실제 요청자인 STUDENT로 기록
  - EnrollmentErrorCode에 신청 없음/잘못된 상태 오류 추가
- 현재 검증 가능 항목:
  - Enrollment `confirm` 도메인 메서드의 `PENDING -> CONFIRMED` 성공
  - Enrollment `confirm` 실패 시 상태 변경 방지
  - PENDING 신청 결제 확정 성공
  - CONFIRMED 상태와 `confirmedAt` 저장
  - ActiveEnrollment와 Course 좌석 점유 유지
  - PAYMENT_CONFIRMED 상태 이력 저장
  - 다른 사용자의 신청 결제 확정 실패
  - 존재하지 않는 신청 결제 확정 실패
  - 이미 CONFIRMED인 신청 재확정 실패
- 검증:
  - 테스트 워크플로우 보강: Enrollment `confirm` 도메인 메서드 성공/실패 규칙 테스트 추가
  - `./gradlew test --tests com.lklass.domain.enrollment.entity.EnrollmentEntityTest` 통과
  - `./gradlew test --tests com.lklass.domain.enrollment.service.EnrollmentServiceTest` 통과
  - `./gradlew test --tests 'com.lklass.domain.enrollment.*'` 통과
  - `./gradlew test` 통과

### Slice 8-B. 수강 취소 use case와 결제/취소 API

- 상태: 완료
- 목표:
  - PENDING/CONFIRMED 신청을 `CANCELLED`로 취소
  - CONFIRMED 신청은 결제 확정 후 7일 이내에만 취소 가능
  - 취소 성공 시 좌석 점유와 활성 신청을 함께 해제
  - 결제 확정/수강 취소 API 응답 계약 고정
- 완료 내역:
  - Enrollment `cancel` 도메인 메서드 추가
  - EnrollmentService `cancel` use case 추가
  - ActiveEnrollment 삭제 repository 메서드 추가
  - `POST /api/enrollments/{enrollmentId}/confirm-payment` API 추가
  - `POST /api/enrollments/{enrollmentId}/cancel` API 추가
  - 취소 가능 기간 만료 오류 추가
- 현재 검증 가능 항목:
  - PENDING 신청 취소 성공
  - CONFIRMED 신청 결제 후 7일 이내 취소 성공
  - CONFIRMED 신청 결제 후 7일 초과 취소 실패
  - 취소 성공 시 ActiveEnrollment 삭제와 Course 좌석 release
  - 다른 사용자의 신청 취소 실패
  - 이미 CANCELLED 상태인 신청 재취소 실패
  - 결제 확정 API 성공/실패 응답
  - 수강 취소 API 성공/실패 응답
- 검증:
  - `./gradlew test --tests com.lklass.domain.enrollment.entity.EnrollmentEntityTest --tests com.lklass.domain.enrollment.service.EnrollmentServiceTest --tests com.lklass.domain.enrollment.controller.EnrollmentControllerTest` 통과

## Slice 9. PENDING 신청 만료

- 유형: AFK
- 선행 조건: Slice 8
- 상태: 완료
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
- 완료 내역:
  - Enrollment `expire` 도메인 메서드 추가
  - EnrollmentService `expirePendingPayments` use case 추가
  - PENDING 만료 대상 조회 repository query 추가
  - EnrollmentExpirationScheduler 추가
  - 목록/만료 조회 인덱스 migration 추가

## Slice 10. 목록, 수강생 조회, 페이지네이션

- 유형: AFK
- 선행 조건: Slice 8
- 상태: 완료
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
- 완료 내역:
  - `GET /api/me/enrollments` API 추가
  - `GET /api/courses/{courseId}/students` API 추가
  - EnrollmentQueryResult projection DTO 추가
  - PageResponse 기반 1-base 페이지 응답 적용
  - Course 수강생 목록은 CoursePermission 기반 관리 권한으로 보호

## Slice 11. 동시성 테스트와 README 마무리

- 유형: HITL
- 선행 조건: Slice 7, Slice 10
- 상태: 진행 중
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
- 완료 내역:
  - 정원 5명 Course에 100건 virtual-thread 동시 신청 통합 테스트 추가
  - README는 사용자 별도 작업으로 남김
- 검증:
  - `./gradlew test --tests 'com.lklass.domain.enrollment.*' --tests com.lklass.global.config.DatabaseFoundationTest` 통과
