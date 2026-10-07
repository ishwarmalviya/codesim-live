import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { Card, Badge, Spinner } from '../components/ui';
import type { CompanyDto } from '../types';

interface RoadmapData {
  skillGapAnalysis?: string;
  dsaRoadmap?: { topic: string; priority: number; description: string }[];
  weeklyPlan?: string[];
  recommendedQuestions?: string[];
  coreSubjects?: { subject: string; priority: number; topics: string[] }[];
}

function readinessTone(score: number | null): 'green' | 'yellow' | 'red' | 'gray' {
  if (score === null) return 'gray';
  if (score >= 70) return 'green';
  if (score >= 40) return 'yellow';
  return 'red';
}

function RoadmapView({ roadmap }: { roadmap: RoadmapData }) {
  return (
    <div className="mt-3 text-sm border-t border-white/10 pt-3 space-y-3 max-h-64 overflow-y-auto">
      {roadmap.skillGapAnalysis && (
        <p className="text-gray-400">{roadmap.skillGapAnalysis}</p>
      )}
      {roadmap.dsaRoadmap && roadmap.dsaRoadmap.length > 0 && (
        <div>
          <div className="text-xs uppercase text-gray-500 font-semibold mb-1">DSA Roadmap</div>
          {roadmap.dsaRoadmap.map((item) => (
            <div key={item.topic} className="flex gap-2 text-gray-300 mb-1">
              <span className="text-brand-400">P{item.priority}</span>
              <span className="font-medium">{item.topic}</span>
              <span className="text-gray-500">— {item.description}</span>
            </div>
          ))}
        </div>
      )}
      {roadmap.weeklyPlan && (
        <div>
          <div className="text-xs uppercase text-gray-500 font-semibold mb-1">Weekly Plan</div>
          <ul className="list-disc list-inside text-gray-400">
            {roadmap.weeklyPlan.map((w, i) => <li key={i}>{w}</li>)}
          </ul>
        </div>
      )}
    </div>
  );
}

export default function Companies() {
  const navigate = useNavigate();
  const [companies, setCompanies] = useState<CompanyDto[] | null>(null);
  const [busyId, setBusyId] = useState<number | null>(null);
  const [expandedId, setExpandedId] = useState<number | null>(null);
  const [roadmaps, setRoadmaps] = useState<Record<number, RoadmapData>>({});

  function load() {
    api.get<CompanyDto[]>('/companies').then((res) => setCompanies(res.data));
  }

  useEffect(load, []);

  async function toggleTarget(c: CompanyDto) {
    setBusyId(c.id);
    try {
      if (c.targeted) {
        await api.delete(`/companies/${c.id}/target`);
      } else {
        await api.post(`/companies/${c.id}/target`);
        const roadmapRes = await api.get<{ roadmap: RoadmapData }>(`/companies/${c.id}/roadmap`);
        setRoadmaps((prev) => ({ ...prev, [c.id]: roadmapRes.data.roadmap }));
      }
      load();
    } finally {
      setBusyId(null);
    }
  }

  async function loadRoadmap(companyId: number) {
    if (roadmaps[companyId]) {
      setExpandedId(expandedId === companyId ? null : companyId);
      return;
    }
    const res = await api.get<{ roadmap: RoadmapData }>(`/companies/${companyId}/roadmap`);
    setRoadmaps((prev) => ({ ...prev, [companyId]: res.data.roadmap }));
    setExpandedId(companyId);
  }

  if (!companies) {
    return <div className="flex justify-center py-20"><Spinner /></div>;
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Target Companies</h1>
        <p className="text-gray-400 text-sm mt-1">
          Pick companies you&apos;re preparing for — we&apos;ll compute AI-driven readiness and show a personalized roadmap.
        </p>
      </div>

      <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-4">
        {companies.map((c) => (
          <Card key={c.id} className="flex flex-col">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="text-2xl">{c.logoEmoji}</span>
                <span className="font-semibold">{c.name}</span>
              </div>
              {c.readinessScore !== null && (
                <Badge tone={readinessTone(c.readinessScore)}>
                  {c.readinessScore.toFixed(0)}% ready
                </Badge>
              )}
            </div>

            <div className="text-xs text-gray-500 mt-2">
              Target Elo: {c.difficultyThreshold} · {c.tags.split(',').filter(Boolean).join(', ')}
            </div>

            <div className="flex flex-wrap gap-1 mt-3">
              {Object.entries(c.requiredTopics)
                .sort((a, b) => b[1] - a[1])
                .slice(0, 4)
                .map(([topic]) => (
                  <Badge key={topic} tone="gray">{topic}</Badge>
                ))}
            </div>

            <button
              onClick={() => toggleTarget(c)}
              disabled={busyId === c.id}
              className={`mt-4 text-sm font-semibold px-4 py-2 rounded-lg transition disabled:opacity-50 ${
                c.targeted ? 'bg-white/10 text-gray-300 hover:bg-white/15' : 'btn-primary text-white'
              }`}
            >
              {busyId === c.id ? '…' : c.targeted ? '✓ Targeted' : 'Set as Target'}
            </button>

            {c.targeted && (
              <>
                <button
                  onClick={() => loadRoadmap(c.id)}
                  className="mt-2 text-xs text-brand-400 hover:text-brand-300 text-left"
                >
                  {expandedId === c.id ? '▼ Hide Roadmap' : '▶ View Interview Roadmap'}
                </button>
                <button
                  onClick={() => navigate(`/practice?companyId=${c.id}&weak=true`)}
                  className="mt-1 text-xs text-emerald-400 hover:text-emerald-300 text-left"
                >
                  Show Recommended Questions →
                </button>
              </>
            )}

            {expandedId === c.id && roadmaps[c.id] && (
              <RoadmapView roadmap={roadmaps[c.id]} />
            )}

            {c.targeted && (
              <Link to={`/interview/start?companyId=${c.id}`} className="mt-3 text-xs text-brand-400 hover:underline">
                Start company-focused interview →
              </Link>
            )}
          </Card>
        ))}
      </div>
    </div>
  );
}
