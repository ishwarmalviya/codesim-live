import { useEffect, useState } from 'react';
import { Bar, Doughnut } from 'react-chartjs-2';
import { Chart as ChartJS, CategoryScale, LinearScale, BarElement, ArcElement, Tooltip, Legend } from 'chart.js';
import { api } from '../../api/client';
import { Card, StatCard, Spinner } from '../../components/ui';
import { AdminDailyQuestionPicker } from './AdminMessages';

ChartJS.register(CategoryScale, LinearScale, BarElement, ArcElement, Tooltip, Legend);

interface AdminDashboardData {
  totalUsers: number;
  activeToday: number;
  totalQuestions: number;
  totalInterviews: number;
  interviewsToday: number;
  avgScore: number;
  recentUsers: { name: string; email: string; createdAt?: string }[];
  topicStats: Record<string, number>;
  trendData: { date: string; count: number }[];
  diffStats: Record<string, number>;
}

export default function AdminDashboard() {
  const [data, setData] = useState<AdminDashboardData | null>(null);

  useEffect(() => {
    api.get<AdminDashboardData>('/admin/dashboard').then((res) => setData(res.data));
  }, []);

  if (!data) {
    return <div className="flex justify-center py-20"><Spinner /></div>;
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Admin Overview</h1>

      <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
        <StatCard label="Total Users" value={data.totalUsers} icon="👥" />
        <StatCard label="Active Today" value={data.activeToday} icon="🟢" />
        <StatCard label="Questions" value={data.totalQuestions} icon="📝" />
        <StatCard label="Total Interviews" value={data.totalInterviews} icon="🎯" />
        <StatCard label="Interviews Today" value={data.interviewsToday} icon="📅" />
        <StatCard label="Avg Score" value={`${data.avgScore}%`} icon="📈" />
      </div>

      <AdminDailyQuestionPicker />

      <div className="grid md:grid-cols-2 gap-4">
        <Card>
          <h3 className="font-semibold mb-3">Interviews (Last 7 Days)</h3>
          <Bar
            data={{
              labels: data.trendData.map((d) => d.date),
              datasets: [{ label: 'Interviews', data: data.trendData.map((d) => d.count), backgroundColor: '#6366f1' }],
            }}
            options={{
              scales: {
                x: { ticks: { color: '#9ca3af' }, grid: { display: false } },
                y: { ticks: { color: '#9ca3af' }, grid: { color: 'rgba(255,255,255,0.05)' } },
              },
              plugins: { legend: { display: false } },
            }}
          />
        </Card>
        <Card>
          <h3 className="font-semibold mb-3">Difficulty Distribution</h3>
          <Doughnut
            data={{
              labels: Object.keys(data.diffStats),
              datasets: [{ data: Object.values(data.diffStats), backgroundColor: ['#6366f1', '#8b5cf6', '#06b6d4', '#f59e0b'] }],
            }}
            options={{ plugins: { legend: { labels: { color: '#e5e7eb' } } } }}
          />
        </Card>
      </div>

      <Card>
        <h3 className="font-semibold mb-3">Questions by Topic</h3>
        <div style={{ height: Math.max(240, Object.keys(data.topicStats).length * 32) }}>
          <Bar
            data={{
              labels: Object.keys(data.topicStats),
              datasets: [{ label: 'Questions', data: Object.values(data.topicStats), backgroundColor: '#8b5cf6' }],
            }}
            options={{
              indexAxis: 'y' as const,
              maintainAspectRatio: false,
              layout: { padding: { left: 8 } },
              scales: {
                x: { ticks: { color: '#9ca3af' }, grid: { color: 'rgba(255,255,255,0.05)' } },
                y: { ticks: { color: '#9ca3af', autoSkip: false }, grid: { display: false } },
              },
              plugins: { legend: { display: false } },
            }}
          />
        </div>
      </Card>
    </div>
  );
}
