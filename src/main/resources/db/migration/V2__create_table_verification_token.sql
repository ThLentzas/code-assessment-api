CREATE TYPE token_type AS ENUM (
    'PASSWORD_RESET',
    'EMAIL_UPDATE'
);

CREATE TABLE verification_token (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    token TEXT NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    type token_type NOT NULL,
    FOREIGN KEY (user_id) REFERENCES app_user(id)
);