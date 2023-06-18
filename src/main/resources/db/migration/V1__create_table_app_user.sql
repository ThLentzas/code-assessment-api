CREATE TABLE IF NOT EXISTS app_user (
    id SERIAL PRIMARY KEY,
    first_name VARCHAR(30) NOT NULL,
    last_name VARCHAR(30) NOT NULL,
    username VARCHAR(30) NOT NULL,
    email VARCHAR(50) NOT NULL,
    password TEXT NOT NULL,
    bio TEXT DEFAULT NULL,
    location VARCHAR(50) DEFAULT NULL,
    company VARCHAR(50) DEFAULT NULL
);