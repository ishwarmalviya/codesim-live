import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client';
import { Card, Badge, Spinner } from '../components/ui';
import { CodeEditor } from '../components/CodeEditor';

interface Submission {
  id: number;
  questionId: number;
  questionTitle: string;
  topic: string;
  difficulty: string;
  status: string;
  statusLabel: string;
  testsPassed: number;
  testsTotal: number;
  score: number;
  submittedAt: string;
  code: string;
  timeTakenSeconds: number;
}

const statusTone: Record<string, 'green' | 'red' | 'yellow' | 'gray'> = {
  accepted: 'green',
  wrong_answer: 'red',
  failed: 'red',
  skipped: 'gray',
};

export default function Submissions() {
  const [submissions, setSubmissions] = useState<Submission[]>([]);
  const [expanded, setExpanded] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.get<Submission[]>('/submissions?page=1&size=50').then((res) => {
      setSubmissions(res.data);
      setLoading(false);
    });
  }, []);

  if (loading) {
    return <div className="flex justify-center py-20"><Spinner /></div>;
  }

  const accepted = submissions.filter((s) => s.status === 'accepted').length;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Submission History</h1>
        <div className="text-sm text-gray-400">
          {submissions.length} submissions · {accepted} accepted
        </div>
      </div>

      {submissions.length === 0 ? (
        <Card className="text-center py-12 text-gray-400">
          No submissions yet. <Link to="/practice" className="text-brand-400">Start practicing</Link>
        </Card>
      ) : (
        <div className="space-y-3">
          {submissions.map((s) => (
            <Card key={s.id} className="overflow-hidden">
              <button
                className="w-full text-left flex items-center justify-between gap-4"
                onClick={() => setExpanded(expanded === s.id ? null : s.id)}
              >
                <div className="flex items-center gap-3 min-w-0">
                  <Badge tone={statusTone[s.status] ?? 'gray'}>{s.statusLabel}</Badge>
                  <span className="font-medium truncate">{s.questionTitle}</span>
                  <Badge tone="gray">{s.topic}</Badge>
                </div>
                <div className="text-xs text-gray-400 shrink-0 text-right">
                  <div>{s.testsPassed}/{s.testsTotal} tests · {s.score.toFixed(1)}/10</div>
                  <div>{new Date(s.submittedAt).toLocaleString()}</div>
                </div>
              </button>
              {expanded === s.id && s.code && (
                <div className="mt-3 rounded-lg overflow-hidden border border-white/10">
                  <CodeEditor value={s.code} readOnly height="200px" />
                </div>
              )}
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
