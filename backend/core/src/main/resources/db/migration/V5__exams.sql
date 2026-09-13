CREATE TABLE exams (
                       id          BIGSERIAL PRIMARY KEY,
                       lesson_id   BIGINT      NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
                       title       VARCHAR(255) NOT NULL,
                       time_limit_minutes INT  NOT NULL DEFAULT 15,
                       created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE exam_questions (
                                id           BIGSERIAL PRIMARY KEY,
                                exam_id      BIGINT  NOT NULL REFERENCES exams(id) ON DELETE CASCADE,
                                question_text TEXT   NOT NULL,
                                max_score    INT     NOT NULL DEFAULT 10,
                                order_index  INT     NOT NULL DEFAULT 0
);

CREATE TABLE exam_attempts (
                               id          BIGSERIAL PRIMARY KEY,
                               exam_id     BIGINT NOT NULL REFERENCES exams(id),
                               user_id     BIGINT NOT NULL REFERENCES users(id),
                               started_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
                               finished_at TIMESTAMPTZ,
                               total_score INT,
                               status      VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS'
                                   CHECK (status IN ('IN_PROGRESS', 'FINISHED', 'EXPIRED'))
);

CREATE TABLE exam_answers (
                              id          BIGSERIAL PRIMARY KEY,
                              attempt_id  BIGINT NOT NULL REFERENCES exam_attempts(id) ON DELETE CASCADE,
                              question_id BIGINT NOT NULL REFERENCES exam_questions(id),
                              answer_text TEXT,
                              score       INT,
                              ai_feedback TEXT
);