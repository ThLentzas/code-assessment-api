CREATE TABLE IF NOT EXISTS app_user (
    id SERIAL,
    first_name VARCHAR(30) NOT NULL,
    last_name VARCHAR(30) NOT NULL,
    username VARCHAR(30) NOT NULL,
    email VARCHAR(50) NOT NULL,
    password TEXT NOT NULL,
    bio VARCHAR(150) DEFAULT NULL,
    location VARCHAR(50) DEFAULT NULL,
    company VARCHAR(50) DEFAULT NULL,
    CONSTRAINT pk_app_user PRIMARY KEY (id)
);
