-- ELO delta tracking for resubmit
ALTER TABLE interview_questions ADD COLUMN IF NOT EXISTS user_elo_delta DOUBLE PRECISION NOT NULL DEFAULT 0;
ALTER TABLE interview_questions ADD COLUMN IF NOT EXISTS question_elo_delta DOUBLE PRECISION NOT NULL DEFAULT 0;

-- Interview-level AI summary (mock interview feedback)
ALTER TABLE interviews ADD COLUMN IF NOT EXISTS ai_summary TEXT DEFAULT '';
ALTER TABLE interviews ADD COLUMN IF NOT EXISTS target_company_id BIGINT REFERENCES companies(id);
ALTER TABLE interviews ADD COLUMN IF NOT EXISTS target_role VARCHAR(100) DEFAULT '';

-- Multi-language question support
ALTER TABLE questions ADD COLUMN IF NOT EXISTS supported_languages TEXT NOT NULL DEFAULT '["python"]';
ALTER TABLE questions ADD COLUMN IF NOT EXISTS starter_code_by_lang TEXT NOT NULL DEFAULT '{}';
UPDATE questions SET starter_code_by_lang = json_build_object('python', starter_code)::text
WHERE starter_code_by_lang = '{}' OR starter_code_by_lang IS NULL;

-- Resume AI analysis
ALTER TABLE resumes ADD COLUMN IF NOT EXISTS ats_score INTEGER;
ALTER TABLE resumes ADD COLUMN IF NOT EXISTS analysis_json TEXT DEFAULT '';

-- Company AI roadmaps
CREATE TABLE IF NOT EXISTS company_roadmaps (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    roadmap_json TEXT NOT NULL DEFAULT '{}',
    generated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, company_id)
);

-- AI-generated roadmap items source
ALTER TABLE roadmap_items ADD COLUMN IF NOT EXISTS source VARCHAR(50) NOT NULL DEFAULT 'gap_based';

-- Assessment topic filter (JSON array)
ALTER TABLE interviews ADD COLUMN IF NOT EXISTS selected_topics TEXT DEFAULT '[]';
