CREATE TYPE quality_metric AS ENUM (
    'COMMENT_RATE',
    'METHOD_SIZE',
    'DUPLICATION',
    'BUG_SEVERITY',
    'TECHNICAL_DEBT_RATIO',
    'RELIABILITY_REMEDIATION_EFFORT',
    'COGNITIVE_COMPLEXITY',
    'CYCLOMATIC_COMPLEXITY',
    'VULNERABILITY_SEVERITY',
    'HOTSPOT_PRIORITY',
    'SECURITY_REMEDIATION_EFFORT'
);

CREATE TYPE quality_attribute AS ENUM (
    'QUALITY',
    'COMPREHENSION',
    'SIMPLICITY',
    'MAINTAINABILITY',
    'RELIABILITY',
    'COMPLEXITY',
    'SECURITY',
    'COMMENT_RATE',
    'METHOD_SIZE',
    'DUPLICATION',
    'BUG_SEVERITY',
    'TECHNICAL_DEBT_RATIO',
    'RELIABILITY_REMEDIATION_EFFORT',
    'COGNITIVE_COMPLEXITY',
    'CYCLOMATIC_COMPLEXITY',
    'VULNERABILITY_SEVERITY',
    'HOTSPOT_PRIORITY',
    'SECURITY_REMEDIATION_EFFORT'
);

CREATE TABLE IF NOT EXISTS analysis (
    id SERIAL,
    user_id INTEGER NOT NULL,
    created_date DATE NOT NULL,
    CONSTRAINT pk_analysis PRIMARY KEY (id),
    CONSTRAINT fk_analysis_user_id FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS analysis_report (
    id SERIAL,
    analysis_id INTEGER NOT NULL,
    report jsonb NOT NULL,
    CONSTRAINT pk_analysis_report PRIMARY KEY (id),
    CONSTRAINT fk_analysis_report_analysis_id FOREIGN KEY (analysis_id) REFERENCES analysis(id) ON DELETE CASCADE
);

CREATE TYPE operator AS ENUM (
    'GT',
    'GTE',
    'LT',
    'LTE',
    'EQ',
    'NEQ'
    );

CREATE TABLE IF NOT EXISTS analysis_constraint (
    id SERIAL,
    analysis_id INTEGER NOT NULL,
    quality_metric quality_metric NOT NULL,
    operator operator NOT NULL,
    threshold double precision NOT NULL,
    CONSTRAINT pk_analysis_constraint PRIMARY KEY (id),
    CONSTRAINT fk_analysis_constraint_analysis_id FOREIGN KEY (analysis_id) REFERENCES analysis(id) ON DELETE CASCADE

);

CREATE TABLE IF NOT EXISTS analysis_preference (
    analysis_id INTEGER NOT NULL,
    quality_attribute quality_attribute NOT NULL,
    weight double precision NOT NULL,
    CONSTRAINT pk_analysis_preference PRIMARY KEY (quality_attribute, analysis_id),
    CONSTRAINT fk_analysis_preference_analysis_id FOREIGN KEY (analysis_id) REFERENCES analysis(id) ON DELETE CASCADE
);


