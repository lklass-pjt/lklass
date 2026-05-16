CREATE TABLE enrollments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    enrolled_at DATETIME(6) NOT NULL,
    confirmed_at DATETIME(6),
    cancelled_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_enrollments_course FOREIGN KEY (course_id) REFERENCES courses (id),
    CONSTRAINT fk_enrollments_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_enrollments_course_id ON enrollments (course_id);
CREATE INDEX idx_enrollments_user_id ON enrollments (user_id);
CREATE INDEX idx_enrollments_status ON enrollments (status);

CREATE TABLE active_enrollments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    enrollment_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_active_enrollments_course FOREIGN KEY (course_id) REFERENCES courses (id),
    CONSTRAINT fk_active_enrollments_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_active_enrollments_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollments (id),
    CONSTRAINT uk_active_enrollments_course_user UNIQUE (course_id, user_id),
    CONSTRAINT uk_active_enrollments_enrollment UNIQUE (enrollment_id)
);

CREATE INDEX idx_active_enrollments_user_id ON active_enrollments (user_id);

CREATE TABLE enrollment_status_histories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    enrollment_id BIGINT NOT NULL,
    from_status VARCHAR(20),
    to_status VARCHAR(20) NOT NULL,
    reason VARCHAR(30) NOT NULL,
    changed_at DATETIME(6) NOT NULL,
    changed_by VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_enrollment_status_histories_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollments (id)
);

CREATE INDEX idx_enrollment_status_histories_enrollment_id ON enrollment_status_histories (enrollment_id);
