ALTER TABLE users ADD COLUMN has_completed_assessment BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE resumes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    raw_text TEXT NOT NULL DEFAULT '',
    parsed_skills TEXT NOT NULL DEFAULT '[]',
    experience_level VARCHAR(50) DEFAULT 'Fresher',
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id)
);

CREATE TABLE companies (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    logo_emoji VARCHAR(10) DEFAULT '🏢',
    required_topics TEXT NOT NULL DEFAULT '{}',
    difficulty_threshold DOUBLE PRECISION NOT NULL DEFAULT 1200.0,
    tags VARCHAR(300) DEFAULT ''
);

CREATE TABLE user_company_targets (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    company_id BIGINT NOT NULL REFERENCES companies(id),
    added_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, company_id)
);

CREATE TABLE company_readiness (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    company_id BIGINT NOT NULL REFERENCES companies(id),
    readiness_score DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    breakdown TEXT NOT NULL DEFAULT '{}',
    computed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, company_id)
);

CREATE TABLE skill_gaps (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    company_id BIGINT REFERENCES companies(id),
    topic VARCHAR(100) NOT NULL,
    required_proficiency DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    actual_proficiency DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    gap_severity DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    computed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE roadmap_items (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    topic VARCHAR(100) NOT NULL,
    question_id BIGINT REFERENCES questions(id),
    priority_rank INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    reason TEXT DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE interview_dna_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    traits TEXT NOT NULL DEFAULT '{}',
    narrative_text TEXT NOT NULL DEFAULT '',
    computed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id)
);

CREATE INDEX idx_skill_gaps_user ON skill_gaps(user_id);
CREATE INDEX idx_roadmap_items_user ON roadmap_items(user_id);
CREATE INDEX idx_company_readiness_user ON company_readiness(user_id);
