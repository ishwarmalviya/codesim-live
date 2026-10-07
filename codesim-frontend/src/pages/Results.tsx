import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import CodeMirror from '@uiw/react-codemirror';
import { python } from '@codemirror/lang-python';
import { oneDark } from '@codemirror/theme-one-dark';
import { api } from '../api/client';
import { Card, Badge, Spinner } from '../components/ui';
import type { ResultsResponse, ReplayEventDto } from '../types';

const statusTone: Record<string, 'green' | 'red' | 'gray'> = {
  solved: 'green',
  failed: 'red',
  skipped: 'gray',
  pending: 'gray',
};

const eventIcon: Record<string, string> = { run: '▶️', submit: '✅', hint: '💡' };

function ReplayTimeline({ iqId }: { iqId: number }) {
  const [events, setEvents] = useState<ReplayEventDto[] | null>(null);
  const [selected, setSelected] = useState<number | null>(null);

  useEffect(() => {
    api.get<ReplayEventDto[]>(`/interviews/questions/${iqId}/replay`).then((res) => {
      setEvents(res.data);
      setSelected(res.data.length > 0 ? res.data.length - 1 : null);
    });
  }, [iqId]);

  if (!events) return <p className="text-sm text-gray-500">Loading replay…</p>;
  if (events.length === 0) return <p className="text-sm text-gray-500">No replay events recorded for this question.</p>;

  const active = selected !== null ? events[selected] : null;

  return (
    <div className="space-y-3">
      <div className="flex gap-2 overflow-x-auto pb-1">
        {events.map((e, i) => (
          <button
            key={e.id}
            onClick={() => setSelected(i)}
            className={`shrink-0 text-xs px-3 py-1.5 rounded-full border transition ${
              selected === i ? 'border-brand-500 bg-brand-500/15 text-brand-300' : 'border-white/10 text-gray-400 hover:bg-white/5'
            }`}
          >
            {eventIcon[e.eventType] ?? '•'} {e.eventType} · {new Date(e.createdAt).toLocaleTimeString()}
          </button>
        ))}
      </div>
      {active && (
        <div className="rounded-lg overflow-hidden border border-white/10">
          <CodeMirror value={active.codeSnapshot} height="200px" theme={oneDark} extensions={[python()]} readOnly editable={false} />
        </div>
      )}
    </div>
  );
}

export default function Results() {
  const { id } = useParams();
  const [data, setData] = useState<ResultsResponse | null>(null);
  const [expanded, setExpanded] = useState<number | null>(null);
  const [replayFor, setReplayFor] = useState<number | null>(null);

  useEffect(() => {
    api.get<ResultsResponse>(`/interviews/${id}/results`).then((res) => setData(res.data));
  }, [id]);

  if (!data) {
    return (
      <div className="flex justify-center py-20">
        <Spinner />
      </div>
    );
  }

  const { interview, eloChange, aiSummary, questions } = data;

  return (
    <div className="space-y-6">
      {aiSummary && (
        <Card className="space-y-4">
          <h3 className="font-semibold text-lg">AI Interview Feedback</h3>
          <p className="text-gray-300">{aiSummary.overall_assessment}</p>
          {aiSummary.hire_recommendation && (
            <Badge tone={aiSummary.hire_recommendation.includes('Yes') ? 'green' : 'yellow'}>
              {aiSummary.hire_recommendation}
            </Badge>
          )}
          <div className="grid md:grid-cols-2 gap-4 text-sm">
            <div>
              <div className="text-xs uppercase text-gray-500 font-semibold mb-1">Strengths</div>
              <ul className="list-disc list-inside text-gray-300">
                {aiSummary.strengths?.map((s, i) => <li key={i}>{s}</li>)}
              </ul>
            </div>
            <div>
              <div className="text-xs uppercase text-gray-500 font-semibold mb-1">Weaknesses</div>
              <ul className="list-disc list-inside text-gray-300">
                {aiSummary.weaknesses?.map((s, i) => <li key={i}>{s}</li>)}
              </ul>
            </div>
          </div>
          {aiSummary.improvements && (
            <div>
              <div className="text-xs uppercase text-gray-500 font-semibold mb-1">Suggested Improvements</div>
              <ul className="list-disc list-inside text-gray-300 text-sm">
                {aiSummary.improvements.map((s, i) => <li key={i}>{s}</li>)}
              </ul>
            </div>
          )}
        </Card>
      )}

      <Card className="text-center py-10">
        <div className="text-5xl font-extrabold grad-text">{interview.scorePercent}%</div>
        <div className="text-gray-400 mt-2">
          {interview.score.toFixed(1)} / {interview.maxScore} points · {interview.questionsAnswered} questions
        </div>
        <div className="mt-3">
          <Badge tone={eloChange >= 0 ? 'green' : 'red'}>
            Elo {eloChange >= 0 ? '+' : ''}{eloChange}
          </Badge>
        </div>
        <div className="mt-6 flex justify-center gap-3">
          <Link to="/interview/start" className="btn-primary text-white font-semibold px-5 py-2.5 rounded-lg">
            Start Another
          </Link>
          <Link to="/dashboard" className="glass-card px-5 py-2.5 rounded-lg font-semibold hover:bg-white/5">
            Dashboard
          </Link>
        </div>
      </Card>

      <div className="space-y-3">
        {questions.map((q) => (
          <Card key={q.iqId}>
            <button className="w-full text-left" onClick={() => setExpanded(expanded === q.iqId ? null : q.iqId)}>
              <div className="flex items-center justify-between flex-wrap gap-2">
                <div className="flex items-center gap-3">
                  <Badge tone={statusTone[q.status] ?? 'gray'}>{q.status}</Badge>
                  <span className="font-semibold">{q.question.title}</span>
                  <Badge tone="gray">{q.question.topic}</Badge>
                </div>
                <div className="text-sm text-gray-400">
                  {q.testsPassed}/{q.testsTotal} tests · {q.score.toFixed(1)}/{q.maxScore} pts
                </div>
              </div>
            </button>

            {expanded === q.iqId && (
              <div className="mt-4 space-y-4">
                {q.userCode && (
                  <div className="rounded-lg overflow-hidden border border-white/10">
                    <CodeMirror value={q.userCode} height="220px" theme={oneDark} extensions={[python()]} readOnly editable={false} />
                  </div>
                )}

                <button
                  onClick={() => setReplayFor(replayFor === q.iqId ? null : q.iqId)}
                  className="text-xs px-3 py-1.5 rounded-lg border border-white/10 hover:bg-white/5"
                >
                  🎬 {replayFor === q.iqId ? 'Hide Replay' : 'View Replay'}
                </button>
                {replayFor === q.iqId && <ReplayTimeline iqId={q.iqId} />}
                {q.review && (
                  <div className="text-sm space-y-2">
                    <p className="text-gray-300">{q.review.overall_assessment}</p>
                    <div className="grid grid-cols-2 gap-3">
                      <div>
                        <div className="text-xs uppercase text-gray-500 font-semibold">Strengths</div>
                        <ul className="list-disc list-inside text-gray-300">
                          {q.review.strengths.map((s, i) => <li key={i}>{s}</li>)}
                        </ul>
                      </div>
                      <div>
                        <div className="text-xs uppercase text-gray-500 font-semibold">Improvements</div>
                        <ul className="list-disc list-inside text-gray-300">
                          {q.review.improvements.map((s, i) => <li key={i}>{s}</li>)}
                        </ul>
                      </div>
                    </div>
                    <div className="text-xs text-gray-400 flex gap-4">
                      <span>Time: {q.review.time_complexity}</span>
                      <span>Space: {q.review.space_complexity}</span>
                    </div>
                    {q.review.optimized_approach && (
                      <p className="text-xs text-gray-400 mt-2"><strong>Optimal approach:</strong> {q.review.optimized_approach}</p>
                    )}
                  </div>
                )}
              </div>
            )}
          </Card>
        ))}
      </div>
    </div>
  );
}
