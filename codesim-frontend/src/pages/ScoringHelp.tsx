import { Link } from 'react-router-dom';
import { Card } from '../components/ui';

export default function ScoringHelp() {
  return (
    <div className="max-w-3xl mx-auto space-y-6 pb-12">
      <h1 className="text-3xl font-bold">How Scoring Works</h1>
      <p className="text-gray-400">
        CodeSim uses transparent, multi-factor scoring to measure your interview readiness. This page explains every metric shown in the dashboard, companies page, and admin panel.
      </p>

      <Card className="space-y-3">
        <h2 className="text-xl font-semibold">Elo Rating</h2>
        <p className="text-sm text-gray-300">
          Your Elo starts at 1000 and updates after every submission using the standard formula:
          expected score = 1 / (1 + 10^((questionElo - userElo) / 400)). User K-factor is 32; question K-factor is 16.
        </p>
        <p className="text-sm text-gray-300">
          Per-question score (0–10) = (tests passed / total) × 10 − hint penalty (0.5 per hint, max 2) ± time bonus (±0.5 based on time vs 30-min limit).
        </p>
      </Card>

      <Card className="space-y-3">
        <h2 className="text-xl font-semibold">Placement Score</h2>
        <p className="text-sm text-gray-300">
          35% Elo component + 25% average company readiness + 20% interview consistency + 20% code quality trend.
          Scores are dampened if you have attempted fewer than 10 questions.
        </p>
      </Card>

      <Card className="space-y-3">
        <h2 className="text-xl font-semibold">Company Readiness</h2>
        <p className="text-sm text-gray-300">
          Multi-factor AI readiness score combining: topic coverage (company-weighted), difficulty progress, accuracy,
          time efficiency, consistency, roadmap completion, and performance trend.
        </p>
        <p className="text-sm text-gray-300">
          Confidence is low until you attempt 15+ questions; displayed % is capped for low-attempt users.
        </p>
      </Card>

      <Card className="space-y-3">
        <h2 className="text-xl font-semibold">Topic Focus Logic</h2>
        <p className="text-sm text-gray-300">
          Each company has topic weights in <code className="text-brand-300">companies_seed.json</code> (e.g. Google emphasizes DP/Graphs; Flipkart emphasizes Arrays/Strings).
          These weights drive readiness and skill-gap analysis.
        </p>
        <p className="text-sm text-gray-300">
          During interviews, question selection uses Elo matching + 1.5× boost for weak topics (proficiency &lt; 40%), not company weights directly — unless you choose Topic Focus mode.
        </p>
      </Card>

      <Card className="space-y-3">
        <h2 className="text-xl font-semibold">Weakness Analysis</h2>
        <p className="text-sm text-gray-300">
          For each company topic: target proficiency = 40 + (weight × 60). Gap severity = target − your actual proficiency.
          Topics with gap &gt; 5 appear on your skill roadmap.
        </p>
      </Card>

      <Card className="space-y-3">
        <h2 className="text-xl font-semibold">Admin Difficulty Distribution</h2>
        <p className="text-sm text-gray-300">
          The admin doughnut chart shows the count of questions in the database grouped by difficulty label (easy/medium/hard/expert) — not user performance.
        </p>
      </Card>

      <Card className="space-y-3">
        <h2 className="text-xl font-semibold">Interview Timer</h2>
        <p className="text-sm text-gray-300">
          Each question has a 30-minute client-side timer. When it reaches zero, your current code is auto-submitted.
          The timer pauses after submit. The interview only ends after all questions are completed or you explicitly exit.
        </p>
      </Card>

      <Link to="/dashboard" className="text-brand-400 hover:underline text-sm">← Back to Dashboard</Link>
    </div>
  );
}
