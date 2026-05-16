CREATE TABLE courses (
    id BIGINT NOT NULL AUTO_INCREMENT,
    creator_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    price DECIMAL(12, 2) NOT NULL,
    capacity INT NOT NULL,
    occupied_count INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    auto_publish_enabled BOOLEAN NOT NULL,
    enrollment_start_at DATETIME(6) NOT NULL,
    enrollment_end_at DATETIME(6) NOT NULL,
    course_start_at DATETIME(6) NOT NULL,
    course_end_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_courses_creator FOREIGN KEY (creator_id) REFERENCES users (id),
    CONSTRAINT chk_courses_price CHECK (price >= 0),
    CONSTRAINT chk_courses_capacity CHECK (capacity >= 1),
    CONSTRAINT chk_courses_occupied_count CHECK (occupied_count >= 0 AND occupied_count <= capacity),
    CONSTRAINT chk_courses_enrollment_period CHECK (enrollment_start_at < enrollment_end_at),
    CONSTRAINT chk_courses_course_period CHECK (course_start_at < course_end_at)
);

CREATE INDEX idx_courses_status ON courses (status);
CREATE INDEX idx_courses_enrollment_period ON courses (enrollment_start_at, enrollment_end_at);
CREATE INDEX idx_courses_creator_id ON courses (creator_id);

CREATE TABLE course_status_histories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    from_status VARCHAR(20),
    to_status VARCHAR(20) NOT NULL,
    reason VARCHAR(30) NOT NULL,
    changed_at DATETIME(6) NOT NULL,
    changed_by VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_course_status_histories_course FOREIGN KEY (course_id) REFERENCES courses (id)
);

CREATE INDEX idx_course_status_histories_course_id ON course_status_histories (course_id);
