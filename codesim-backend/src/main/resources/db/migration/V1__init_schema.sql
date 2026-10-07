CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(256) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    elo_rating DOUBLE PRECISION NOT NULL DEFAULT 1000.0,
    total_interviews INTEGER NOT NULL DEFAULT 0,
    total_questions_solved INTEGER NOT NULL DEFAULT 0,
    avg_score DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_active TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_active_account BOOLEAN NOT NULL DEFAULT TRUE,
    bio TEXT DEFAULT '',
    college VARCHAR(200) DEFAULT '',
    preferred_language VARCHAR(50) DEFAULT 'python'
);

CREATE TABLE questions (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    difficulty VARCHAR(20) NOT NULL,
    elo_rating DOUBLE PRECISION NOT NULL DEFAULT 1000.0,
    topic VARCHAR(100) NOT NULL,
    subtopic VARCHAR(100) DEFAULT '',
    function_signature TEXT DEFAULT '',
    starter_code TEXT DEFAULT '',
    solution_code TEXT DEFAULT '',
    test_cases TEXT DEFAULT '[]',
    hints TEXT DEFAULT '[]',
    constraints TEXT DEFAULT '',
    examples TEXT DEFAULT '',
    time_complexity VARCHAR(50) DEFAULT '',
    space_complexity VARCHAR(50) DEFAULT '',
    tags VARCHAR(300) DEFAULT '',
    times_attempted INTEGER NOT NULL DEFAULT 0,
    times_solved INTEGER NOT NULL DEFAULT 0,
    avg_time_seconds DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT REFERENCES users(id)
);

CREATE TABLE interviews (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    interview_type VARCHAR(50) NOT NULL DEFAULT 'adaptive',
    selected_topic VARCHAR(100) DEFAULT '',
    total_questions INTEGER NOT NULL DEFAULT 5,
    questions_answered INTEGER NOT NULL DEFAULT 0,
    score DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    max_score DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    elo_before DOUBLE PRECISION NOT NULL DEFAULT 1000.0,
    elo_after DOUBLE PRECISION NOT NULL DEFAULT 1000.0,
    overall_feedback TEXT DEFAULT '',
    strengths TEXT DEFAULT '',
    improvements TEXT DEFAULT ''
);

CREATE TABLE interview_questions (
    id BIGSERIAL PRIMARY KEY,
    interview_id BIGINT NOT NULL REFERENCES interviews(id),
    question_id BIGINT NOT NULL REFERENCES questions(id),
    question_order INTEGER NOT NULL DEFAULT 1,
    user_code TEXT DEFAULT '',
    score DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    max_score DOUBLE PRECISION NOT NULL DEFAULT 10.0,
    tests_passed INTEGER NOT NULL DEFAULT 0,
    tests_total INTEGER NOT NULL DEFAULT 0,
    time_taken_seconds INTEGER NOT NULL DEFAULT 0,
    hints_used INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    submitted_at TIMESTAMPTZ,
    ai_feedback TEXT DEFAULT '',
    code_quality_score DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    complexity_analysis TEXT DEFAULT '',
    execution_output TEXT DEFAULT ''
);

CREATE TABLE topic_performances (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    topic VARCHAR(100) NOT NULL,
    questions_attempted INTEGER NOT NULL DEFAULT 0,
    questions_solved INTEGER NOT NULL DEFAULT 0,
    avg_score DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    proficiency DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    last_practiced TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, topic)
);

CREATE INDEX idx_questions_topic ON questions(topic);
CREATE INDEX idx_questions_elo ON questions(elo_rating);
CREATE INDEX idx_interviews_user ON interviews(user_id);
CREATE INDEX idx_interview_questions_interview ON interview_questions(interview_id);
CREATE INDEX idx_topic_perf_user ON topic_performances(user_id);
