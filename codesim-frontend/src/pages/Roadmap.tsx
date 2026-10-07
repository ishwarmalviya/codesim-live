import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client';
import { Card, Badge, Spinner, PrimaryButton } from '../components/ui';
import type { RoadmapItemDto, SkillGapDto } from '../types';

export default function Roadmap() {
  const [gaps, setGaps] = useState<SkillGapDto[] | null>(null);
  const [items, setItems] = useState<RoadmapItemDto[] | null>(null);

  useEffect(() => {
    api.get<SkillGapDto[]>('/skill-gaps').then((res) => setGaps(res.data));
    api.get<RoadmapItemDto[]>('/roadmap').then((res) => setItems(res.data));
  }, []);

  if (!gaps || !items) {
    return <div className="flex justify-center py-20"><Spinner /></div>;
  }

  const hasAiItems = items.some((i) => i.reason?.includes('AI'));

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between flex-wrap gap-4">
        <div>
          <h1 className="text-2xl font-bold">Skill Gaps & Roadmap</h1>
          <p className="text-gray-400 text-sm mt-1">
            Based on your targeted companies (or a general baseline if you haven't picked one yet).
          </p>
        </div>
        <Link to="/roadmap/setup">
          <PrimaryButton>Generate AI Roadmap</PrimaryButton>
        </Link>
      </div>

      {!hasAiItems && items.length === 0 && (
        <Card className="text-sm text-gray-400">
          Complete the <Link to="/roadmap/setup" className="text-brand-400">roadmap setup wizard</Link> for a personalized 4-week AI study plan.
        </Card>
      )}

      <Card>
        <h3 className="font-semibold mb-3">Skill Gaps</h3>
        {gaps.length === 0 ? (
          <p className="text-sm text-gray-500">No gaps computed yet — target a company or complete an interview.</p>
        ) : (
          <div className="space-y-3">
            {gaps.map((g) => (
              <div key={g.topic}>
                <div className="flex justify-between text-sm mb-1">
                  <span className="font-medium">{g.topic}</span>
                  <span className="text-gray-400">
                    {g.actualProficiency.toFixed(0)}% / {g.requiredProficiency.toFixed(0)}% target
                  </span>
                </div>
                <div className="h-2 bg-white/10 rounded-full overflow-hidden relative">
                  <div className="h-full grad-brand rounded-full" style={{ width: `${g.actualProficiency}%` }} />
                  <div
                    className="absolute top-0 bottom-0 border-l-2 border-white/40"
                    style={{ left: `${g.requiredProficiency}%` }}
                  />
                </div>
              </div>
            ))}
          </div>
        )}
      </Card>

      <Card>
        <h3 className="font-semibold mb-3">Personalized Roadmap</h3>
        {items.length === 0 ? (
          <p className="text-sm text-gray-500">No roadmap items yet — your gaps are small, or none computed yet.</p>
        ) : (
          <div className="space-y-2">
            {items.map((item) => (
              <div key={item.id} className="flex items-center justify-between bg-white/5 rounded-lg px-4 py-3">
                <div>
                  <div className="flex items-center gap-2">
                    <span className="w-6 h-6 rounded-full bg-brand-500/20 text-brand-300 text-xs flex items-center justify-center font-bold">
                      {item.priorityRank}
                    </span>
                    <span className="font-medium">{item.questionTitle ?? item.topic}</span>
                    <Badge tone="gray">{item.topic}</Badge>
                  </div>
                  <div className="text-xs text-gray-500 mt-1">{item.reason}</div>
                </div>
                {item.questionId && (
                  <Link to={`/practice/${item.questionId}`} className="text-sm text-brand-400 hover:text-brand-300 shrink-0">
                    Practice →
                  </Link>
                )}
              </div>
            ))}
          </div>
        )}
      </Card>
    </div>
  );
}
