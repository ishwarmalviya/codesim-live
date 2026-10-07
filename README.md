# CodeSim AI — Adaptive Coding Interview Simulator & Dynamic Review Engine

A full-stack platform for practicing coding interviews with an Elo-based adaptive engine, AI-powered code review, and placement-readiness intelligence (skill gap detection, company readiness, personalized roadmaps, and a recruiter dashboard).

## Stack

- **Backend:** Spring Boot 3.5 (Java 17), Spring Data JPA, Spring Security (JWT), PostgreSQL, Flyway
- **Frontend:** React 19 + TypeScript + Vite, Tailwind CSS v4, Chart.js, CodeMirror 6
- **Code execution:** Python via local sandbox (`runner.py`); C/C++/Java/JavaScript via **self-hosted Judge0 CE** (Docker on `:2358`)
- **AI:** OpenAI GPT-4o for code review, hints, interview summaries, and the Interview DNA narrative (falls back to rule-based mock responses if `OPENAI_API_KEY` isn't set — the app is fully usable without a key)

## Project layout

```
codesim-backend/    Spring Boot API (port 8080)
codesim-frontend/   React SPA (port 5173, dev) — talks to the API via /api
legacy-flask/       The original Flask/SQLite prototype, kept for reference only
docker-compose.yml  One-command Postgres + backend + frontend stack (untested locally — no Docker on this machine; validate before relying on it)
```

## Running locally (without Docker)

### 1. PostgreSQL

```bash
brew install postgresql@16
brew services start postgresql@16
createdb codesim
psql -d codesim -c "CREATE ROLE codesim_app LOGIN PASSWORD 'codesim_dev_pw';"
psql -d codesim -c "GRANT ALL PRIVILEGES ON DATABASE codesim TO codesim_app;"
psql -d codesim -c "ALTER DATABASE codesim OWNER TO codesim_app;"
```

### 2. Backend

Requires Java 17+ and Python 3 on PATH (the code-execution sandbox shells out to `python3`).

```bash
cd codesim-backend
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn spring-boot:run
```

Runs on `http://localhost:8080`. On first boot it seeds the schema (Flyway), 36 questions, 15 companies, and three demo accounts. Optional env vars:

- `OPENAI_API_KEY` — enables real AI code review/hints/DNA narratives (otherwise a deterministic mock is used)
- `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` — override Postgres connection (defaults match the setup above)
- `JWT_SECRET` — override the dev JWT signing key
- `PYTHON_BIN` — override the Python interpreter used for code execution (default `python3`)
- `JUDGE0_API_URL` — Judge0 CE URL (default `http://localhost:2358`)
- `JUDGE0_ENABLED` — enable multi-language execution via Judge0 (default `true`)

### 2b. Judge0 CE (multi-language execution)

Self-hosted Judge0 runs in Docker — no RapidAPI or external API key needed.

```bash
# Judge0 only (when running backend/frontend locally)
chmod +x scripts/start-judge0.sh
./scripts/start-judge0.sh

# Or manually:
docker compose up -d judge0-db judge0-redis
sleep 12
docker compose up -d judge0-server judge0-workers
```

Verify: `curl http://localhost:2358/about`

Supported in the code editor: **Python** (local runner), **JavaScript, Java, C, C++** (Judge0).

### 3. Frontend

```bash
cd codesim-frontend
npm install
npm run dev
```

Runs on `http://localhost:5173` and proxies `/api/*` to the backend (see `vite.config.ts`).

### Demo accounts

| Role      | Email                  | Password       |
|-----------|------------------------|----------------|
| Admin     | admin@codesim.ai       | admin123       |
| User      | demo@codesim.ai        | demo123        |
| Recruiter | recruiter@codesim.ai   | recruiter123   |

## Running with Docker

```bash
docker compose up --build
```

This starts:
- **Postgres** (CodeSim) on `:5432`
- **Judge0 CE** on `:2358` (self-hosted, open source)
- **Backend** on `:8080` (connects to Judge0 at `http://judge0-server:2358`)
- **Frontend** on `:5173`

Set `OPENAI_API_KEY` in your shell or `.env` before running to enable real AI features.

Judge0 requires `privileged: true` for sandboxing (standard for Judge0 CE). First startup may take a few minutes while compiler images download.

### Judge0 only (local backend dev)

```bash
./scripts/start-judge0.sh
# Then run backend with JUDGE0_API_URL=http://localhost:2358 (default)
```

## Feature map

**Core (parity with the original Flask app):** auth, Elo-adaptive interview engine, sandboxed Python execution with AST-based static analysis, AI code review/hints, dashboards, leaderboard, full admin panel (users, question bank CRUD, analytics).

**Candidate intelligence:** Resume upload/parsing with starter skill-proficiency boost, Initial Assessment (calibration interview), Interview DNA (behavioral profile: speed, persistence, code-quality trend, favorite topics), Company Selection with per-company topic-weight profiles, Company Readiness scoring, Skill Gap Detection, Personalized Roadmap.

**Analytics & recruiting:** Interview Replay Analytics (per-question event timeline: hints/submissions with code snapshots), Placement Intelligence Engine (combined Elo/readiness/consistency/quality score), Recruiter Dashboard (candidate search by placement score, shortlisting with notes).

**Continuous improvement loop:** completing any interview automatically recomputes topic proficiency, skill gaps, roadmap, Interview DNA, and placement score for that user.

## Known limitations

- Docker Compose includes self-hosted Judge0 CE for C/C++/Java/JS execution.
- OpenAI-backed features fall back to deterministic mocks without an API key — functional but less personalized.
- Python uses the local `runner.py` sandbox; other languages require Judge0 Docker to be running.
