import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Line, Radar } from 'react-chartjs-2';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  RadialLinearScale,
  Tooltip,
  Filler,
} from 'chart.js';
import { api } from '../api/client';
import { Card, StatCard, Spinner, PrimaryButton, Badge } from '../components/ui';
import type { DashboardResponse, PlacementScoreDto } from '../types';

const ALL_TOPICS_DASHBOARD = [
  'Arrays', 'Strings', 'Trees', 'Graphs', 'Dynamic Programming',
  'Sorting', 'Binary Search', 'Linked Lists', 'Recursion', 'Hashing',
];

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, RadialLinearScale, Tooltip, Filler);

function placementTone(score: number): 'green' | 'yellow' | 'red' {
  if (score >= 65) return 'green';
  if (score >= 40) return 'yellow';
  return 'red';
}

export default function Dashboard() {
  const [data, setData] = useState<DashboardResponse | null>(null);
  const [placement, setPlacement] = useState<PlacementScoreDto | null>(null);
  const [recommendation, setRecommendation] = useState<{
    available?: boolean;
    title?: string;
    topic?: string;
    reason?: string;
    questionId?: number;
  } | null>(null);
  const [placementTips, setPlacementTips] = useState<{ tips?: string[]; priority?: string } | null>(null);
  const [dailyQuestion, setDailyQuestion] = useState<{
    available?: boolean;
    solved?: boolean;
    question?: { id: number; title: string; difficulty: string; topic: string };
    expiresAt?: string;
  } | null>(null);
  const navigate = useNavigate();

  useEffect(() => {
    api.get<DashboardResponse>('/dashboard').then((res) => setData(res.data));
    api.get<PlacementScoreDto>('/placement-score').then((res) => setPlacement(res.data));
    api.get('/recommendations/next-question').then((res) => setRecommendation(res.data));
    api.get('/placement-score/improvements').then((res) => setPlacementTips(res.data)).catch(() => {});
    api.get('/daily-question').then((res) => setDailyQuestion(res.data)).catch(() => {});
  }, []);

  async function startAssessment() {
    navigate('/assessment/setup');
  }

  if (!data) {
    return (
      <div className="flex justify-center py-20">
        <Spinner />
      </div>
    );
  }

  const { user, recentInterviews, topicPerfs, chartData, radarData, streak, rank } = data;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between flex-wrap gap-4">
        <div>
          <h1 className="text-2xl font-bold">Welcome back, {user.name.split(' ')[0]} 👋</h1>
          <p className="text-gray-400 text-sm mt-1">Rank #{rank} · {streak > 0 ? `${streak}-day streak 🔥` : 'Start a streak today'}</p>
        </div>
        <Link to="/interview/start" className="btn-primary text-white font-semibold px-5 py-2.5 rounded-lg">
          Start New Interview
        </Link>
      </div>

      {dailyQuestion?.available && dailyQuestion.question && (
        <Card className="border border-brand-500/40 bg-brand-500/5">
          <div className="flex items-center justify-between flex-wrap gap-4">
            <div>
              <div className="text-xs uppercase text-brand-400 font-semibold mb-1">Daily Challenge</div>
              <div className="font-bold text-lg">{dailyQuestion.question.title}</div>
              <div className="text-sm text-gray-400 mt-1">
                {dailyQuestion.question.difficulty} · {dailyQuestion.question.topic}
                {dailyQuestion.solved && <span className="text-emerald-400 ml-2">✓ Solved</span>}
              </div>
            </div>
            {!dailyQuestion.solved && (
              <Link
                to={`/practice/${dailyQuestion.question.id}`}
                className="btn-primary text-white font-semibold px-5 py-2.5 rounded-lg text-sm"
              >
                Solve Today&apos;s Challenge
              </Link>
            )}
          </div>
        </Card>
      )}

      {!user.hasCompletedAssessment && (
        <Card className="flex items-center justify-between flex-wrap gap-4 border border-brand-500/30">
          <div>
            <h3 className="font-semibold">🎯 Take your Initial Assessment</h3>
            <p className="text-sm text-gray-400 mt-1">
              A quick 6-question diagnostic across topics to properly calibrate your Elo and skill matrix
              (right now you're starting from the default baseline).
            </p>
          </div>
          <PrimaryButton onClick={startAssessment}>
            Start Assessment
          </PrimaryButton>
        </Card>
      )}

      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <StatCard label="Elo Rating" value={Math.round(user.eloRating)} icon="⚡" />
        <StatCard label="Interviews" value={user.totalInterviews} icon="🎯" />
        <StatCard label="Problems Solved" value={user.totalQuestionsSolved} icon="✅" />
        <StatCard label="Avg Score" value={`${user.avgScore.toFixed(1)}%`} icon="📈" />
      </div>

      {placement && (
        <Card>
          <div className="flex items-center justify-between flex-wrap gap-4">
            <div>
              <h3 className="font-semibold flex items-center gap-2">
                🚀 Placement Readiness
                <Badge tone={placementTone(placement.overallScore)}>{placement.overallScore.toFixed(0)} / 100</Badge>
              </h3>
              <p className="text-xs text-gray-500 mt-1">
                {placement.breakdown.formula || '35% Elo + 25% readiness + 20% consistency + 20% quality'}
                {placement.breakdown.confidence === 'low' && ' · Low confidence — complete more questions'}
              </p>
              <Link to="/help/scoring" className="text-xs text-brand-400 mt-1 hover:underline inline-block">
                How is this calculated?
              </Link>
              {placementTips?.tips && placementTips.tips.length > 0 && (
                <div className="text-xs text-gray-400 mt-2">
                  <p className="font-semibold text-gray-300">Quick tips:</p>
                  <ul className="list-disc list-inside">{placementTips.tips.slice(0, 2).map((t, i) => <li key={i}>{t}</li>)}</ul>
                </div>
              )}
            </div>
            <div className="flex gap-4 text-xs text-gray-400">
              <div>Elo <div className="text-gray-200 font-semibold">{placement.breakdown.eloComponent.toFixed(0)}</div></div>
              <div>Readiness <div className="text-gray-200 font-semibold">{placement.breakdown.readinessComponent.toFixed(0)}</div></div>
              <div>Consistency <div className="text-gray-200 font-semibold">{placement.breakdown.consistencyComponent.toFixed(0)}</div></div>
              <div>Quality <div className="text-gray-200 font-semibold">{placement.breakdown.qualityComponent.toFixed(0)}</div></div>
            </div>
          </div>
        </Card>
      )}

      {recommendation?.available && (
        <Card className="border border-brand-500/30">
          <h3 className="font-semibold">Recommended for You</h3>
          <p className="text-sm text-gray-300 mt-1">{recommendation.title} · {recommendation.topic}</p>
          <p className="text-xs text-gray-400 mt-1">{recommendation.reason}</p>
          <Link
            to={recommendation.questionId ? `/practice/${recommendation.questionId}` : '/practice'}
            className="inline-block mt-3 text-sm text-brand-400 hover:text-brand-300"
          >
            Practice Now →
          </Link>
        </Card>
      )}

      <div className="grid md:grid-cols-2 gap-4">
        <Card>
          <h3 className="font-semibold mb-3">Score & Elo Trend</h3>
          {chartData.labels.length === 0 ? (
            <p className="text-sm text-gray-500 py-8 text-center">Complete an interview to see your trend.</p>
          ) : (
            <Line
              data={{
                labels: chartData.labels,
                datasets: [
                  {
                    label: 'Score %',
                    data: chartData.scores,
                    borderColor: '#6366f1',
                    backgroundColor: 'rgba(99,102,241,0.15)',
                    fill: true,
                    tension: 0.3,
                    yAxisID: 'y',
                  },
                  {
                    label: 'Elo',
                    data: chartData.elos,
                    borderColor: '#a78bfa',
                    backgroundColor: 'rgba(167,139,250,0.1)',
                    fill: false,
                    tension: 0.3,
                    yAxisID: 'y1',
                  },
                ],
              }}
              options={{
                scales: {
                  x: { ticks: { color: '#9ca3af' }, grid: { color: 'rgba(255,255,255,0.05)' } },
                  y: { position: 'left', ticks: { color: '#9ca3af' }, grid: { color: 'rgba(255,255,255,0.05)' } },
                  y1: { position: 'right', ticks: { color: '#a78bfa' }, grid: { drawOnChartArea: false } },
                },
                plugins: { legend: { labels: { color: '#e5e7eb' } } },
              }}
            />
          )}
        </Card>

        <Card>
          <h3 className="font-semibold mb-3">Topic Proficiency</h3>
          <Radar
            data={{
              labels: radarData.topics,
              datasets: [
                {
                  label: 'Proficiency',
                  data: radarData.values,
                  borderColor: '#8b5cf6',
                  backgroundColor: 'rgba(139,92,246,0.2)',
                },
              ],
            }}
            options={{
              scales: {
                r: {
                  angleLines: { color: 'rgba(255,255,255,0.1)' },
                  grid: { color: 'rgba(255,255,255,0.1)' },
                  pointLabels: { color: '#9ca3af', font: { size: 10 } },
                  ticks: { display: false, backdropColor: 'transparent' },
                  suggestedMin: 0,
                  suggestedMax: 100,
                },
              },
              plugins: { legend: { display: false } },
            }}
          />
        </Card>
      </div>

      <div className="grid md:grid-cols-2 gap-4">
        <Card>
          <h3 className="font-semibold mb-3">Strengths</h3>
          <div className="space-y-2">
            {topicPerfs.filter((tp) => tp.proficiency >= 60).length === 0 && (
              <p className="text-sm text-gray-500">No strengths yet — keep practicing!</p>
            )}
            {topicPerfs.filter((tp) => tp.proficiency >= 60).map((tp) => (
              <div key={tp.topic}>
                <div className="flex justify-between text-sm mb-1">
                  <span>{tp.topic}</span>
                  <span className="text-emerald-400">{tp.proficiency.toFixed(0)}%</span>
                </div>
                <div className="text-xs text-gray-500">{tp.questionsAttempted} attempted · {tp.successRate?.toFixed(0) ?? 0}% success</div>
              </div>
            ))}
          </div>
        </Card>

        <Card>
          <h3 className="font-semibold mb-3">Weaknesses</h3>
          <div className="space-y-2">
            {ALL_TOPICS_DASHBOARD.map((topic) => {
              const tp = topicPerfs.find((t) => t.topic === topic);
              const prof = tp?.proficiency ?? 0;
              const attempted = tp?.questionsAttempted ?? 0;
              const isWeak = prof < 40 || attempted === 0;
              if (!isWeak && prof >= 40) return null;
              return (
                <div key={topic}>
                  <div className="flex justify-between text-sm mb-1">
                    <span>{topic}</span>
                    <span className="text-gray-400">
                      {attempted === 0 ? '0% — Not attempted' : `${prof.toFixed(0)}%`}
                    </span>
                  </div>
                  <div className="h-2 bg-white/10 rounded-full overflow-hidden">
                    <div className="h-full grad-brand rounded-full" style={{ width: `${prof}%` }} />
                  </div>
                </div>
              );
            })}
          </div>
        </Card>
      </div>

      <Card>
        <h3 className="font-semibold mb-3">Weakest Topics</h3>
        <div className="space-y-2">
          {topicPerfs.length === 0 && <p className="text-sm text-gray-500">No topic data yet — complete some interviews.</p>}
          {topicPerfs.slice(0, 5).map((tp) => (
            <div key={tp.topic}>
              <div className="flex justify-between text-sm mb-1">
                <span>{tp.topic}</span>
                <span className="text-gray-400">{tp.proficiency.toFixed(0)}%</span>
              </div>
              <div className="h-2 bg-white/10 rounded-full overflow-hidden">
                <div className="h-full grad-brand rounded-full" style={{ width: `${tp.proficiency}%` }} />
              </div>
            </div>
          ))}
        </div>
      </Card>

      <Card>
        <h3 className="font-semibold mb-3">Recent Interviews</h3>
        {recentInterviews.length === 0 ? (
          <p className="text-sm text-gray-500">No interviews yet.</p>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="text-gray-400 text-left border-b border-white/10">
                <th className="pb-2">ID</th>
                <th className="pb-2">Type</th>
                <th className="pb-2">Score</th>
                <th className="pb-2">Elo</th>
                <th className="pb-2"></th>
              </tr>
            </thead>
            <tbody>
              {recentInterviews.map((i) => (
                <tr key={i.id} className="border-b border-white/5">
                  <td className="py-2">#{i.id}</td>
                  <td className="py-2 capitalize">{i.interviewType}</td>
                  <td className="py-2">{i.scorePercent}%</td>
                  <td className="py-2">{Math.round(i.eloAfter)}</td>
                  <td className="py-2 text-right">
                    <Link to={`/interview/${i.id}/results`} className="text-brand-400 hover:text-brand-300">View →</Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>
    </div>
  );
}
