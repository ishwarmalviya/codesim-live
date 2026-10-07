import { useEffect, useState } from 'react';
import { api } from '../../api/client';
import { Card, Badge, Spinner, Input } from '../../components/ui';
import type { CandidateDto } from '../../types';

interface Page<T> {
  content: T[];
  totalElements: number;
}

function tone(score: number | null): 'green' | 'yellow' | 'red' | 'gray' {
  if (score === null) return 'gray';
  if (score >= 65) return 'green';
  if (score >= 40) return 'yellow';
  return 'red';
}

export default function RecruiterCandidates() {
  const [page, setPage] = useState<Page<CandidateDto> | null>(null);
  const [minScore, setMinScore] = useState('');
  const [noteDrafts, setNoteDrafts] = useState<Record<number, string>>({});

  function load() {
    api.get<Page<CandidateDto>>('/recruiter/candidates', {
      params: { minScore: minScore || undefined },
    }).then((res) => setPage(res.data));
  }

  useEffect(load, [minScore]);

  async function toggleShortlist(c: CandidateDto) {
    if (c.shortlisted) {
      await api.delete(`/recruiter/candidates/${c.userId}/shortlist`);
    } else {
      await api.post(`/recruiter/candidates/${c.userId}/shortlist`, { note: noteDrafts[c.userId] ?? '' });
    }
    load();
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between flex-wrap gap-3">
        <h1 className="text-2xl font-bold">🔍 Candidate Search</h1>
        <div className="w-56">
          <Input
            type="number"
            placeholder="Min placement score"
            value={minScore}
            onChange={(e) => setMinScore(e.target.value)}
          />
        </div>
      </div>

      {!page ? (
        <div className="flex justify-center py-10"><Spinner /></div>
      ) : (
        <div className="grid md:grid-cols-2 gap-4">
          {page.content.map((c) => (
            <Card key={c.userId}>
              <div className="flex items-center justify-between">
                <div>
                  <div className="font-semibold">{c.name}</div>
                  <div className="text-xs text-gray-500">{c.email}</div>
                  <div className="text-xs text-gray-500">{c.college || 'N/A'}</div>
                </div>
                {c.placementScore !== null && (
                  <Badge tone={tone(c.placementScore)}>{c.placementScore.toFixed(0)} / 100</Badge>
                )}
              </div>
              <div className="flex gap-4 text-xs text-gray-400 mt-3">
                <span>Elo {Math.round(c.eloRating)}</span>
                <span>{c.skillLevel}</span>
              </div>
              {!c.shortlisted && (
                <Input
                  placeholder="Note (optional)"
                  className="mt-3"
                  value={noteDrafts[c.userId] ?? ''}
                  onChange={(e) => setNoteDrafts((d) => ({ ...d, [c.userId]: e.target.value }))}
                />
              )}
              <button
                onClick={() => toggleShortlist(c)}
                className={`mt-3 w-full text-sm font-semibold px-4 py-2 rounded-lg transition ${
                  c.shortlisted ? 'border border-white/10 hover:bg-white/5' : 'btn-primary text-white'
                }`}
              >
                {c.shortlisted ? 'Remove from Shortlist' : '★ Shortlist'}
              </button>
            </Card>
          ))}
          {page.content.length === 0 && <p className="text-sm text-gray-500">No candidates match this filter yet.</p>}
        </div>
      )}
    </div>
  );
}
