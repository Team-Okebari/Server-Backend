ALTER TABLE note_cover
    ALTER COLUMN main_image_url TYPE VARCHAR(500);

ALTER TABLE note_overview
    ALTER COLUMN image_url TYPE VARCHAR(500);

ALTER TABLE note_process
    ALTER COLUMN image_url TYPE VARCHAR(500);

ALTER TABLE note_reminder_pot
    ALTER COLUMN payload_main_image_url TYPE VARCHAR(500);

-- Alter notes_head table
ALTER TABLE notes_head
    ALTER COLUMN source_url TYPE VARCHAR(500);