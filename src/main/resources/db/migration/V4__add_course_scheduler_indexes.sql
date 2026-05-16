CREATE INDEX idx_courses_auto_open_targets
    ON courses (status, auto_publish_enabled, enrollment_start_at, enrollment_end_at);

CREATE INDEX idx_courses_auto_close_targets
    ON courses (status, enrollment_end_at);
