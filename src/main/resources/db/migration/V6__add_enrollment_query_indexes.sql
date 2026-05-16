CREATE INDEX idx_enrollments_user_created_at ON enrollments (user_id, created_at);
CREATE INDEX idx_enrollments_course_created_at ON enrollments (course_id, created_at);
CREATE INDEX idx_enrollments_pending_expiration ON enrollments (status, enrolled_at);
