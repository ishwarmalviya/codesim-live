import { useEffect, useState } from 'react';
import { api } from '../../api/client';
import { Card, Spinner } from '../../components/ui';
import type { CandidateDto } from '../../types';

export default function RecruiterShortlist() {
  const [candidates, setCandidates] = useState<CandidateDto[] | null>(null);

  function load() {
    api.get<CandidateDto[]>('/recruiter/shortlist').then((res) => setCandidates(res.data));
  }

  useEffect(load, []);

  async function remove(userId: number) {
    await api.delete(`/recruiter/candidates/${userId}/shortlist`);
    load();
  }

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold">⭐ Shortlist</h1>

      {!candidates ? (
        <div className="flex justify-center py-10"><Spinner /></div>
      ) : candidates.length === 0 ? (
        <p className="text-sm text-gray-500">No candidates shortlisted yet — search and shortlist from Candidate Search.</p>
      ) : (
        <div className="space-y-3">
          {candidates.map((c) => (
            <Card key={c.userId} className="flex items-center justify-between flex-wrap gap-3">
              <div>
                <div className="font-semibold">{c.name}</div>
                <div className="text-xs text-gray-500">{c.email} · {c.college || 'N/A'}</div>
                {c.note && <div className="text-sm text-gray-300 mt-2 italic">"{c.note}"</div>}
              </div>
              <button
                onClick={() => remove(c.userId)}
                className="text-xs text-red-400 hover:text-red-300 border border-white/10 px-3 py-1.5 rounded-lg"
              >
                Remove
              </button>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
