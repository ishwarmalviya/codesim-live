import { useEffect, useState } from 'react';
import { api } from '../api/client';
import { Card, Spinner, Badge } from '../components/ui';
import type { LeaderboardEntry } from '../types';

export default function Leaderboard() {
  const [entries, setEntries] = useState<LeaderboardEntry[] | null>(null);

  useEffect(() => {
    api.get<LeaderboardEntry[]>('/leaderboard').then((res) => setEntries(res.data));
  }, []);

  return (
    <div className="max-w-2xl mx-auto">
      <h1 className="text-2xl font-bold mb-6">🏆 Leaderboard</h1>
      <Card>
        {!entries ? (
          <div className="flex justify-center py-10"><Spinner /></div>
        ) : (
          <div className="space-y-1">
            {entries.map((e) => (
              <div key={e.rank} className="flex items-center justify-between py-2.5 px-2 rounded-lg hover:bg-white/5">
                <div className="flex items-center gap-3">
                  <span className="w-6 text-center font-bold text-gray-400">
                    {e.rank === 1 ? '🥇' : e.rank === 2 ? '🥈' : e.rank === 3 ? '🥉' : e.rank}
                  </span>
                  <div>
                    <div className="font-medium">{e.name}</div>
                    <div className="text-xs text-gray-500">{e.college}</div>
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <Badge tone="gray">{e.level}</Badge>
                  <span className="font-bold text-brand-300 w-14 text-right">{e.elo}</span>
                </div>
              </div>
            ))}
          </div>
        )}
      </Card>
    </div>
  );
}
