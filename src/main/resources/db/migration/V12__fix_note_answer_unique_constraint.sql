-- Drop the incorrect unique constraint on question_id only, if it exists
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'note_answer_question_id_key' AND conrelid = 'note_answer'::regclass) THEN
        ALTER TABLE note_answer DROP CONSTRAINT note_answer_question_id_key;
    END IF;
END
$$;
