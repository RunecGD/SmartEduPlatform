CREATE TABLE study_groups (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL UNIQUE,
    curator_id BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_study_groups_curator ON study_groups(curator_id);
CREATE TABLE study_group_students (
    group_id BIGINT NOT NULL REFERENCES study_groups(id) ON DELETE CASCADE,
    student_id BIGINT NOT NULL REFERENCES users(id),
    PRIMARY KEY (group_id, student_id),
    UNIQUE (student_id)
);
