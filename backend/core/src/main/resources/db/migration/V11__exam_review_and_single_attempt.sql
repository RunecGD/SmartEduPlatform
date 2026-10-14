-- Preserve old attempts and answers, including historical repeated attempts.
ALTER TABLE exam_attempts DROP CONSTRAINT IF EXISTS exam_attempts_status_check;
ALTER TABLE exam_attempts ADD CONSTRAINT exam_attempts_status_check
    CHECK (status IN ('IN_PROGRESS', 'SUBMITTED', 'FINISHED', 'EXPIRED'));

-- A permanent reservation enforces one attempt, without deleting old history.
CREATE TABLE exam_attempt_reservations (
    exam_id BIGINT NOT NULL REFERENCES exams(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (exam_id, user_id)
);
INSERT INTO exam_attempt_reservations (exam_id, user_id)
SELECT DISTINCT exam_id, user_id FROM exam_attempts;

CREATE FUNCTION reserve_single_exam_attempt() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'UPDATE' THEN
        IF NEW.exam_id IS DISTINCT FROM OLD.exam_id OR NEW.user_id IS DISTINCT FROM OLD.user_id THEN
            RAISE EXCEPTION 'Cannot change the owner or exam of an attempt' USING ERRCODE = '23514';
        END IF;
        RETURN NEW;
    END IF;
    INSERT INTO exam_attempt_reservations (exam_id, user_id)
    VALUES (NEW.exam_id, NEW.user_id) ON CONFLICT DO NOTHING;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Only one attempt per student and exam is allowed' USING ERRCODE = '23505';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER exam_attempt_reservation
BEFORE INSERT OR UPDATE OF exam_id, user_id ON exam_attempts
FOR EACH ROW EXECUTE FUNCTION reserve_single_exam_attempt();
