
CREATE TABLE courses (
                         id          BIGSERIAL PRIMARY KEY,
                         teacher_id  BIGINT      NOT NULL REFERENCES users(id),
                         title       VARCHAR(255) NOT NULL,
                         description TEXT,
                         category    VARCHAR(100),
                         status      VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                             CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
                         created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
                         updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE course_modules (
                                id          BIGSERIAL PRIMARY KEY,
                                course_id   BIGINT      NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
                                title       VARCHAR(255) NOT NULL,
                                order_index INT         NOT NULL DEFAULT 0,
                                created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE lessons (
                         id          BIGSERIAL PRIMARY KEY,
                         module_id   BIGINT      NOT NULL REFERENCES course_modules(id) ON DELETE CASCADE,
                         type        VARCHAR(20) NOT NULL DEFAULT 'TEXT'
                             CHECK (type IN ('TEXT', 'VIDEO', 'QUIZ', 'CODE')),
                         title       VARCHAR(255) NOT NULL,
                         content     TEXT,
                         order_index INT         NOT NULL DEFAULT 0,
                         created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE enrollments (
                             id           BIGSERIAL PRIMARY KEY,
                             user_id      BIGINT      NOT NULL REFERENCES users(id),
                             course_id    BIGINT      NOT NULL REFERENCES courses(id),
                             progress_pct INT         NOT NULL DEFAULT 0 CHECK (progress_pct BETWEEN 0 AND 100),
                             enrolled_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
                             UNIQUE (user_id, course_id)
);

CREATE TABLE lesson_progress (
                                 id            BIGSERIAL PRIMARY KEY,
                                 enrollment_id BIGINT     NOT NULL REFERENCES enrollments(id) ON DELETE CASCADE,
                                 lesson_id     BIGINT     NOT NULL REFERENCES lessons(id),
                                 completed_at  TIMESTAMPTZ,
                                 score         INT,
                                 UNIQUE (enrollment_id, lesson_id)
);

-- Индексы под типовые запросы (каталог, личный кабинет, проверка записи)
CREATE INDEX idx_courses_teacher   ON courses(teacher_id);
CREATE INDEX idx_courses_status    ON courses(status);
CREATE INDEX idx_modules_course    ON course_modules(course_id);
CREATE INDEX idx_lessons_module    ON lessons(module_id);
CREATE INDEX idx_enrollments_user  ON enrollments(user_id);
CREATE INDEX idx_progress_lesson   ON lesson_progress(lesson_id);