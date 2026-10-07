CREATE TABLE interview_replay_events (
    id BIGSERIAL PRIMARY KEY,
    interview_question_id BIGINT NOT NULL REFERENCES interview_questions(id),
    event_type VARCHAR(20) NOT NULL, -- run, submit, hint
    code_snapshot TEXT DEFAULT '',
    payload TEXT DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE placement_scores (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    overall_score DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    breakdown TEXT NOT NULL DEFAULT '{}',
    computed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id)
);

CREATE TABLE recruiter_shortlists (
    id BIGSERIAL PRIMARY KEY,
    recruiter_id BIGINT NOT NULL REFERENCES users(id),
    candidate_id BIGINT NOT NULL REFERENCES users(id),
    note TEXT DEFAULT '',
    added_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (recruiter_id, candidate_id)
);

CREATE INDEX idx_replay_events_iq ON interview_replay_events(interview_question_id);
CREATE INDEX idx_placement_scores_user ON placement_scores(user_id);
CREATE INDEX idx_recruiter_shortlists_recruiter ON recruiter_shortlists(recruiter_id);
