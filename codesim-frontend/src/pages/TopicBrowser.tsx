import { useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { api } from '../api/client';
import { Card, Badge, Spinner } from '../components/ui';
import type { RoadmapItemDto } from '../types';

interface TopicQuestion {
  id: number;
  title: string;
  difficulty: string;
  topic: string;
  status: 'solved' | 'attempted' | 'unsolved';
  acceptanceRate: number;
}

interface TopicGroup {
  name: string;
  total: number;
  solved: number;
  questions: TopicQuestion[];
}

export default function TopicBrowser() {
  const [topics, setTopics] = useState<TopicGroup[]>([]);
  const [roadmap, setRoadmap] = useState<RoadmapItemDto[]>([]);
  const [selectedTopic, setSelectedTopic] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const companyId = searchParams.get('companyId');
  const weakOnly = searchParams.get('weak') === 'true';

  useEffect(() => {
    Promise.all([
      api.get<{ topics: TopicGroup[] }>('/questions/by-topic'),
      api.get<RoadmapItemDto[]>('/roadmap'),
    ]).then(([topicRes, roadmapRes]) => {
      setTopics(topicRes.data.topics);
      setRoadmap(roadmapRes.data.filter((r) => r.status === 'pending'));
      if (topicRes.data.topics.length > 0) {
        setSelectedTopic(topicRes.data.topics[0].name);
      }
      setLoading(false);
    });
  }, [companyId, weakOnly]);

  if (loading) {
    return <div className="flex justify-center py-20"><Spinner /></div>;
  }

  const active = topics.find((t) => t.name === selectedTopic);

  return (
    <div className="flex gap-4 min-h-[calc(100vh-6rem)]">
      <aside className="w-56 shrink-0 glass-card p-4 space-y-2">
        <h2 className="font-bold text-sm uppercase text-gray-500 mb-3">Topics</h2>
        {topics.map((t) => (
          <button
            key={t.name}
            onClick={() => setSelectedTopic(t.name)}
            className={`w-full text-left px-3 py-2 rounded-lg text-sm transition ${
              selectedTopic === t.name ? 'bg-brand-500/20 text-brand-300' : 'hover:bg-white/5 text-gray-300'
            }`}
          >
            <div className="flex justify-between">
              <span>{t.name}</span>
              <span className="text-xs text-gray-500">{t.solved}/{t.total}</span>
            </div>
            <div className="h-1 bg-white/10 rounded-full mt-1 overflow-hidden">
              <div className="h-full grad-brand" style={{ width: `${t.total ? (t.solved / t.total) * 100 : 0}%` }} />
            </div>
          </button>
        ))}

        {roadmap.length > 0 && (
          <div className="pt-4 border-t border-white/10 mt-4">
            <h3 className="text-xs uppercase text-gray-500 font-semibold mb-2">Interview Roadmap</h3>
            {roadmap.slice(0, 4).map((r) => (
              <div key={r.id} className="text-xs text-gray-400 mb-2">
                <span className="text-brand-300">#{r.priorityRank}</span> {r.topic}
              </div>
            ))}
            <Link to="/roadmap" className="text-xs text-brand-400 hover:underline">View full roadmap →</Link>
          </div>
        )}
      </aside>

      <div className="flex-1">
        <div className="flex items-center justify-between mb-4">
          <h1 className="text-2xl font-bold">{active?.name ?? 'Practice'}</h1>
          {companyId && <Badge tone="gray">Filtered for company prep</Badge>}
        </div>

        <Card className="overflow-hidden p-0">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-white/10 text-gray-400 text-left">
                <th className="px-4 py-3 font-medium">Status</th>
                <th className="px-4 py-3 font-medium">Title</th>
                <th className="px-4 py-3 font-medium">Difficulty</th>
                <th className="px-4 py-3 font-medium">Acceptance</th>
                <th className="px-4 py-3 font-medium" />
              </tr>
            </thead>
            <tbody>
              {active?.questions.map((q) => (
                <tr key={q.id} className="border-b border-white/5 hover:bg-white/5">
                  <td className="px-4 py-3">
                    {q.status === 'solved' ? '✅' : q.status === 'attempted' ? '🟡' : '⬜'}
                  </td>
                  <td className="px-4 py-3 font-medium">{q.title}</td>
                  <td className="px-4 py-3">
                    <Badge tone={q.difficulty === 'easy' ? 'green' : q.difficulty === 'medium' ? 'yellow' : 'red'}>
                      {q.difficulty}
                    </Badge>
                  </td>
                  <td className="px-4 py-3 text-gray-400">{q.acceptanceRate?.toFixed(0) ?? 0}%</td>
                  <td className="px-4 py-3 text-right">
                    <button
                      onClick={() => navigate(`/practice/${q.id}`)}
                      className="text-xs text-brand-400 hover:underline"
                    >
                      Solve →
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </Card>
      </div>
    </div>
  );
}
