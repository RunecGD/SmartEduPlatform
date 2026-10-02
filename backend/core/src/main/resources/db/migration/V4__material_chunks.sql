CREATE TABLE material_chunks (
                                 id          BIGSERIAL PRIMARY KEY,
                                 material_id BIGINT NOT NULL REFERENCES materials(id) ON DELETE CASCADE,
                                 chunk_index INT    NOT NULL,
                                 content     TEXT   NOT NULL,
                                 embedding   vector(768) NOT NULL        -- 768 = размерность nomic-embed-text
);

CREATE INDEX idx_chunks_material ON material_chunks(material_id);