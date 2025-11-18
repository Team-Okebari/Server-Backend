CREATE TABLE membership_inducement_image
(
    id         BIGSERIAL PRIMARY KEY,
    image_url  VARCHAR(500) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL
);

INSERT INTO membership_inducement_image (id, image_url, created_at, updated_at)
VALUES (1,
        'https://sparki-note-images.s3.ap-northeast-2.amazonaws.com/membership-inducement-image/resource_membership.png',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
