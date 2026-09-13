CREATE TABLE materials (
                           id          BIGSERIAL PRIMARY KEY,
                           lesson_id   BIGINT       NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
                           file_name   VARCHAR(512) NOT NULL,
                           content_type VARCHAR(255),
                           object_key  VARCHAR(512) NOT NULL,     -- путь в MinIO, НЕ URL
                           size_bytes  BIGINT,
                           uploaded_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_materials_lesson ON materials(lesson_id);