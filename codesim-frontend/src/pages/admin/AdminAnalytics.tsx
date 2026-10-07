import { useEffect, useState } from 'react';
import { Line, Bar } from 'react-chartjs-2';
import { Chart as ChartJS, CategoryScale, LinearScale, PointElement, LineElement, BarElement, Tooltip } from 'chart.js';
import { api } from '../../api/client';
import { Card, Spinner } from '../../components/ui';
import type { UserDto } from '../../types';

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, BarElement, Tooltip);

interface TopicStat {
  topic: string;
  total: number;
  avgElo: number;
  attempts: number;
  solved: number;
}

interface AnalyticsData {
  topUsers: UserDto[];
  topicStats: TopicStat[];
  scoreDist: Record<string, number>;
  userGrowth: { date: string; count: number }[];
}

export default function AdminAnalytics() {
  const [data, setData] = useState<AnalyticsData | null>(null);

  useEffect(() => {
    api.get<AnalyticsData>('/admin/analytics').then((res) => setData(res.data));
  }, []);

  if (!data) return <div className="flex justify-center py-20"><Spinner /></div>;

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Platform Analytics</h1>

      <div className="grid md:grid-cols-2 gap-4">
        <Card>
          <h3 className="font-semibold mb-3">User Growth (30 Days)</h3>
          <Line
            data={{
              labels: data.userGrowth.map((d) => d.date),
              datasets: [{ label: 'New Users', data: data.userGrowth.map((d) => d.count), borderColor: '#06b6d4', tension: 0.3 }],
            }}
            options={{
              scales: { x: { ticks: { color: '#9ca3af', maxTicksLimit: 8 }, grid: { display: false } }, y: { ticks: { color: '#9ca3af' } } },
              plugins: { legend: { display: false } },
            }}
          />
        </Card>
        <Card>
          <h3 className="font-semibold mb-3">Score Distribution</h3>
          <Bar
            data={{
              labels: Object.keys(data.scoreDist),
              datasets: [{ label: 'Interviews', data: Object.values(data.scoreDist), backgroundColor: '#6366f1' }],
            }}
            options={{
              scales: { x: { ticks: { color: '#9ca3af' }, grid: { display: false } }, y: { ticks: { color: '#9ca3af' } } },
              plugins: { legend: { display: false } },
            }}
          />
        </Card>
      </div>

      <Card>
        <h3 className="font-semibold mb-3">Topic-wise Stats</h3>
        <table className="w-full text-sm">
          <thead>
            <tr className="text-gray-400 text-left border-b border-white/10">
              <th className="pb-2">Topic</th>
              <th className="pb-2">Questions</th>
              <th className="pb-2">Avg Elo</th>
              <th className="pb-2">Attempts</th>
              <th className="pb-2">Solved</th>
            </tr>
          </thead>
          <tbody>
            {data.topicStats.map((t) => (
              <tr key={t.topic} className="border-b border-white/5">
                <td className="py-2">{t.topic}</td>
                <td className="py-2">{t.total}</td>
                <td className="py-2">{t.avgElo}</td>
                <td className="py-2">{t.attempts}</td>
                <td className="py-2">{t.solved}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>

      <Card>
        <h3 className="font-semibold mb-3">Top Performers</h3>
        <div className="grid md:grid-cols-2 gap-2">
          {data.topUsers.map((u, i) => (
            <div key={u.id} className="flex justify-between bg-white/5 rounded-lg px-3 py-2 text-sm">
              <span>#{i + 1} {u.name}</span>
              <span className="text-brand-300 font-semibold">{Math.round(u.eloRating)}</span>
            </div>
          ))}
        </div>
      </Card>
    </div>
  );
}
