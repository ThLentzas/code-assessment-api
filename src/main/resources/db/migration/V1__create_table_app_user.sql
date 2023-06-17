CREATE TABLE IF NOT EXISTS app_user (
    id SERIAL PRIMARY KEY,
    first_name VARCHAR(30) NOT NULL,
    last_name VARCHAR(30) NOT NULL,
    username VARCHAR(30) NOT NULL,
    email VARCHAR(80) NOT NULL,
    password TEXT NOT NULL,
    bio TEXT,
    location VARCHAR(80),
    company VARCHAR(80)
);