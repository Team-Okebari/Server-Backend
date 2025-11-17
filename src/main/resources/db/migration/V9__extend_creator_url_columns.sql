ALTER TABLE note_creator
    ADD COLUMN IF NOT EXISTS profile_image_url VARCHAR(500);

ALTER TABLE note_creator
    ALTER COLUMN profile_image_url TYPE VARCHAR(500);

ALTER TABLE note_creator
    ALTER COLUMN instagram_url TYPE VARCHAR(500);

ALTER TABLE note_creator
    ALTER COLUMN youtube_url TYPE VARCHAR(500);

ALTER TABLE note_creator
    ALTER COLUMN behance_url TYPE VARCHAR(500);

ALTER TABLE note_creator
    ALTER COLUMN x_url TYPE VARCHAR(500);

ALTER TABLE note_creator
    ALTER COLUMN blog_url TYPE VARCHAR(500);

ALTER TABLE note_creator
    ALTER COLUMN news_url TYPE VARCHAR(500);
