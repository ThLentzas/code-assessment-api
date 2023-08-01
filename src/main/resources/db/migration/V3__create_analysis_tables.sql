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
    id           SERIAL PRIMARY KEY,
    user_id      INTEGER   NOT NULL,
    created_date TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS analysis_report (
    id          SERIAL PRIMARY KEY,
    analysis_id INTEGER NOT NULL,
    report      jsonb   NOT NULL,
    FOREIGN KEY (analysis_id) REFERENCES analysis (id) ON DELETE CASCADE
);

CREATE TYPE operator AS ENUM (
    'GT',
    'GTE',
    'LT',
    'LTE',
    'EQ',
    'NEQ'
);

/*
    constraint is a reserved key word similar to user.
 */
CREATE TABLE IF NOT EXISTS analysis_constraint (
    analysis_id    INTEGER          NOT NULL,
    quality_metric quality_metric   NOT NULL,
    operator       operator         NOT NULL,
    threshold      double precision NOT NULL,
    FOREIGN KEY (analysis_id) REFERENCES analysis (id) ON DELETE CASCADE,
    PRIMARY KEY (quality_metric, analysis_id)
);

CREATE TABLE IF NOT EXISTS analysis_preference (
    analysis_id       INTEGER           NOT NULL,
    quality_attribute quality_attribute NOT NULL,
    weight            double precision  NOT NULL,
    FOREIGN KEY (analysis_id) REFERENCES analysis (id) ON DELETE CASCADE,
    PRIMARY KEY (quality_attribute, analysis_id)
);