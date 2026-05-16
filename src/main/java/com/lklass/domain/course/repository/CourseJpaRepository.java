package com.lklass.domain.course.repository;

import com.lklass.domain.course.dto.CourseQueryResult;
import com.lklass.domain.course.entity.Course;
import com.lklass.domain.course.entity.CourseStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseJpaRepository extends JpaRepository<Course, Long> {

    // 목록/상세 화면에는 creator 이름만 필요하므로 Course-User 연관관계 대신 조회 전용 projection으로 조합한다.
    @Query(
            value = """
                    select new com.lklass.domain.course.dto.CourseQueryResult(
                        c.id,
                        c.creatorId,
                        u.name,
                        c.title,
                        c.description,
                        c.price.amount,
                        c.capacity.value,
                        c.occupiedCount,
                        c.status,
                        c.autoPublishEnabled,
                        c.enrollmentPeriod.startAt,
                        c.enrollmentPeriod.endAt,
                        c.coursePeriod.startAt,
                        c.coursePeriod.endAt
                    )
                      from Course c
                      join User u on u.id = c.creatorId
                     where (:status is null or c.status = :status)
                    """,
            countQuery = """
                    select count(c)
                      from Course c
                      join User u on u.id = c.creatorId
                     where (:status is null or c.status = :status)
                    """
    )
    Page<CourseQueryResult> findCourseResults(@Param("status") CourseStatus status, Pageable pageable);

    @Query("""
            select new com.lklass.domain.course.dto.CourseQueryResult(
                c.id,
                c.creatorId,
                u.name,
                c.title,
                c.description,
                c.price.amount,
                c.capacity.value,
                c.occupiedCount,
                c.status,
                c.autoPublishEnabled,
                c.enrollmentPeriod.startAt,
                c.enrollmentPeriod.endAt,
                c.coursePeriod.startAt,
                c.coursePeriod.endAt
            )
              from Course c
              join User u on u.id = c.creatorId
             where c.id = :courseId
            """)
    Optional<CourseQueryResult> findCourseResultById(@Param("courseId") Long courseId);

    boolean existsByIdAndCreatorId(Long id, Long creatorId);
}
