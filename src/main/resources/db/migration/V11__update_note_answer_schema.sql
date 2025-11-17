-- Make user_id NOT NULL (PostgreSQL syntax)
ALTER TABLE note_answer
    ALTER COLUMN user_id SET NOT NULL;

-- Add a new unique constraint on (question_id, user_id)
ALTER TABLE note_answer
    ADD CONSTRAINT UQ_note_answer_question_user UNIQUE (question_id, user_id);