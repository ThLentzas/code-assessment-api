CREATE TYPE quality_metric AS ENUM (
    'RELIABILITY',
    'SECURITY',
    'MAINTAINABILITY',
    'COGNITIVE_COMPLEXITY',
    'CYCLOMATIC_COMPLEXITY',
    'TECHNICAL_DEPT'
);

CREATE TABLE IF NOT EXISTS analysis (
    id SERIAL PRIMARY KEY,
    user_id  INTEGER NOT NULL,
    created_date TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS analysis_report (
    id SERIAL PRIMARY KEY,
    analysis_id INTEGER NOT NULL,
    report jsonb NOT NULL,
    FOREIGN KEY (analysis_id) REFERENCES analysis(id)
);

CREATE TYPE operator AS ENUM (
    '>',
    '>=',
    '<',
    '<=',
    '==',
    '<>'
);

CREATE TABLE IF NOT EXISTS quality_metric_details (
    analysis_id INTEGER NOT NULL,
    quality_metric quality_metric NOT NULL,
    operator operator NOT NULL,
    threshold  double precision NOT NULL,
    weight double precision,
    FOREIGN KEY (analysis_id) REFERENCES analysis(id),
    PRIMARY KEY (quality_metric, analysis_id)
);