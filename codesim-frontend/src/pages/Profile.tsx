import { useEffect, useState } from 'react';
import { api } from '../api/client';
import { Card, StatCard, Spinner, PrimaryButton, Input, Badge, Select } from '../components/ui';
import { useAuthStore } from '../store/auth';
import type { ProfileResponse, InterviewDnaDto } from '../types';

export default function Profile() {
  const [data, setData] = useState<ProfileResponse | null>(null);
  const [dna, setDna] = useState<InterviewDnaDto | null>(null);
  const updateUser = useAuthStore((s) => s.updateUser);
  const [editing, setEditing] = useState(false);
  const [name, setName] = useState('');
  const [bio, setBio] = useState('');
  const [college, setCollege] = useState('');
  const [preferredLanguage, setPreferredLanguage] = useState('python');
  const [newPassword, setNewPassword] = useState('');
  const [saving, setSaving] = useState(false);

  function load() {
    api.get<ProfileResponse>('/profile').then((res) => {
      setData(res.data);
      setName(res.data.user.name);
      setBio(res.data.user.bio);
      setCollege(res.data.user.college);
      setPreferredLanguage(res.data.user.preferredLanguage || 'python');
    });
    api.get<InterviewDnaDto>('/interview-dna').then((res) => setDna(res.data));
  }

  useEffect(load, []);

  async function handleSave() {
    setSaving(true);
    try {
      const res = await api.put<ProfileResponse>('/profile', {
        name, bio, college, preferredLanguage, newPassword: newPassword || undefined,
      });
      setData(res.data);
      updateUser(res.data.user);
      setNewPassword('');
      setEditing(false);
    } finally {
      setSaving(false);
    }
  }

  if (!data) {
    return (
      <div className="flex justify-center py-20">
        <Spinner />
      </div>
    );
  }

  const { user, topicPerfs, allInterviews, aiInsights, rank } = data;

  return (
    <div className="space-y-6">
      <Card>
        <div className="flex items-start justify-between flex-wrap gap-4">
          <div className="flex items-center gap-4">
            <div className="w-16 h-16 rounded-full grad-brand flex items-center justify-center text-2xl font-bold">
              {user.name.charAt(0).toUpperCase()}
            </div>
            <div>
              <h1 className="text-xl font-bold">{user.name}</h1>
              <p className="text-sm text-gray-400">{user.college || 'No institution set'}</p>
              <p className="text-sm text-gray-400">{user.bio}</p>
            </div>
          </div>
          <button onClick={() => setEditing(!editing)} className="glass-card px-4 py-2 rounded-lg text-sm hover:bg-white/5">
            {editing ? 'Cancel' : 'Edit Profile'}
          </button>
        </div>

        {editing && (
          <div className="mt-6 space-y-3 border-t border-white/10 pt-4">
            <Input value={name} onChange={(e) => setName(e.target.value)} placeholder="Name" />
            <Input value={college} onChange={(e) => setCollege(e.target.value)} placeholder="College" />
            <div>
              <label className="text-sm font-medium mb-2 block">Preferred Coding Language</label>
              <Select value={preferredLanguage} onChange={(e) => setPreferredLanguage(e.target.value)}>
                <option value="python">Python</option>
                <option value="javascript">JavaScript</option>
                <option value="java">Java</option>
                <option value="cpp">C++</option>
                <option value="c">C</option>
              </Select>
            </div>
            <textarea
              value={bio}
              onChange={(e) => setBio(e.target.value)}
              placeholder="Bio"
              maxLength={500}
              className="w-full bg-white/5 border border-white/10 rounded-lg px-3 py-2.5 text-sm focus:outline-none focus:border-brand-500"
              rows={3}
            />
            <Input
              type="password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              placeholder="New password (optional)"
            />
            <PrimaryButton onClick={handleSave} disabled={saving}>{saving ? 'Saving…' : 'Save Changes'}</PrimaryButton>
          </div>
        )}
      </Card>

      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <StatCard label="Elo Rating" value={Math.round(user.eloRating)} icon="⚡" />
        <StatCard label="Rank" value={`#${rank}`} icon="🏆" />
        <StatCard label="Level" value={user.skillLevel} icon="🎓" />
        <StatCard label="Avg Score" value={`${user.avgScore.toFixed(1)}%`} icon="📈" />
      </div>

      {dna && dna.traits.status !== 'insufficient_data' && (
        <Card>
          <h3 className="font-semibold mb-2">🧬 Interview DNA</h3>
          <p className="text-sm text-gray-300">{dna.narrativeText}</p>
          <div className="flex flex-wrap gap-2 mt-3">
            {dna.traits.speed_profile && <Badge tone="brand">{dna.traits.speed_profile} solver</Badge>}
            {dna.traits.persistence_profile && <Badge tone="gray">{dna.traits.persistence_profile}</Badge>}
            {dna.traits.code_quality_trend && <Badge tone="green">quality {dna.traits.code_quality_trend}</Badge>}
            {dna.traits.favorite_topics?.map((t) => <Badge key={t} tone="yellow">{t}</Badge>)}
          </div>
        </Card>
      )}

      {aiInsights && (
        <Card>
          <h3 className="font-semibold mb-2">🤖 AI Insights</h3>
          <p className="text-sm text-gray-300">{aiInsights.summary}</p>
          {aiInsights.focus_areas && aiInsights.focus_areas.length > 0 && (
            <div className="mt-3">
              <div className="text-xs uppercase text-gray-500 font-semibold">Focus Areas</div>
              <ul className="list-disc list-inside text-sm text-gray-300">
                {aiInsights.focus_areas.map((a, i) => <li key={i}>{a}</li>)}
              </ul>
            </div>
          )}
        </Card>
      )}

      <Card>
        <h3 className="font-semibold mb-3">Topic Performance</h3>
        <div className="grid md:grid-cols-2 gap-3">
          {topicPerfs.map((tp) => (
            <div key={tp.topic} className="bg-white/5 rounded-lg p-3">
              <div className="flex justify-between text-sm mb-1">
                <span className="font-medium">{tp.topic}</span>
                <span className="text-gray-400">{tp.proficiency.toFixed(0)}%</span>
              </div>
              <div className="h-1.5 bg-white/10 rounded-full overflow-hidden">
                <div className="h-full grad-brand rounded-full" style={{ width: `${tp.proficiency}%` }} />
              </div>
              <div className="text-xs text-gray-500 mt-1">{tp.questionsSolved}/{tp.questionsAttempted} solved</div>
            </div>
          ))}
        </div>
      </Card>

      <Card>
        <h3 className="font-semibold mb-3">Interview History</h3>
        <table className="w-full text-sm">
          <thead>
            <tr className="text-gray-400 text-left border-b border-white/10">
              <th className="pb-2">Date</th>
              <th className="pb-2">Topic</th>
              <th className="pb-2">Score</th>
              <th className="pb-2">Questions</th>
            </tr>
          </thead>
          <tbody>
            {allInterviews.map((i) => (
              <tr key={i.id} className="border-b border-white/5">
                <td className="py-2">{i.completedAt ? new Date(i.completedAt).toLocaleDateString() : '-'}</td>
                <td className="py-2 capitalize">{i.selectedTopic}</td>
                <td className="py-2">{i.scorePercent}%</td>
                <td className="py-2">{i.questionsAnswered}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>
    </div>
  );
}
