ALTER TABLE note_creator
	ADD COLUMN job_title VARCHAR(60);

UPDATE note_creator
SET job_title = bio
WHERE job_title IS NULL;
