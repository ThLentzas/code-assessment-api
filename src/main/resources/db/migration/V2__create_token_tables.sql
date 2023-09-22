CREATE TABLE password_reset_token (
    id SERIAL,
    user_id INTEGER NOT NULL,
    token TEXT NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    CONSTRAINT pk_password_reset_token PRIMARY KEY (id),
    CONSTRAINT fk_password_reset_token_user_id FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE
);

CREATE TABLE email_update_token (
    id SERIAL,
    user_id INTEGER NOT NULL,
    token TEXT NOT NULL,
    email VARCHAR(50) NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    CONSTRAINT pk_email_update_token PRIMARY KEY (id),
    CONSTRAINT fk_email_update_token_user_id FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE
);
