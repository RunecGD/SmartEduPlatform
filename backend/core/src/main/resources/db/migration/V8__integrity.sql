-- Run after existing core tables have been created. Does not remove duplicate data.
-- A unique index intentionally fails if historical duplicates are present.
CREATE UNIQUE INDEX IF NOT EXISTS ux_exam_active_attempt ON exam_attempts(exam_id,user_id) WHERE status='IN_PROGRESS';
CREATE UNIQUE INDEX IF NOT EXISTS ux_exam_question_order ON exam_questions(exam_id,order_index);
CREATE UNIQUE INDEX IF NOT EXISTS ux_exam_answer_question ON exam_answers(attempt_id,question_id);
CREATE UNIQUE INDEX IF NOT EXISTS ux_material_object_key ON materials(object_key);
CREATE UNIQUE INDEX IF NOT EXISTS ux_material_chunk_index ON material_chunks(material_id,chunk_index);
