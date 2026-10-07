import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { api } from '../api/client';
import { Card, PrimaryButton, Select } from '../components/ui';
import type { CompanyDto, InterviewDto } from '../types';

export default function StartInterview() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const preselectedCompanyId = searchParams.get('companyId');

  const [topics, setTopics] = useState<string[]>([]);
  const [companies, setCompanies] = useState<CompanyDto[]>([]);
  const [type, setType] = useState<'adaptive' | 'topic'>('adaptive');
  const [topic, setTopic] = useState('adaptive');
  const [numQuestions, setNumQuestions] = useState(5);
  const [targetCompanyId, setTargetCompanyId] = useState<number | ''>(
    preselectedCompanyId ? Number(preselectedCompanyId) : '',
  );
  const [targetRole, setTargetRole] = useState('Software Engineer');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    api.get<string[]>('/interviews/topics').then((res) => setTopics(res.data));
    api.get<CompanyDto[]>('/companies').then((res) => setCompanies(res.data));
  }, []);

  async function handleStart() {
    setLoading(true);
    try {
      const res = await api.post<InterviewDto>('/interviews/start', {
        type,
        topic: type === 'adaptive' ? 'adaptive' : topic,
        numQuestions,
        targetCompanyId: targetCompanyId || undefined,
        targetRole: targetRole || undefined,
      });
      navigate(`/interview/${res.data.id}`);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="max-w-xl mx-auto">
      <h1 className="text-2xl font-bold mb-2">Interview Mode</h1>
      <p className="text-sm text-gray-400 mb-6">
        Timed multi-question mock interviews. To solve a single problem, use{' '}
        <button type="button" onClick={() => navigate('/practice')} className="text-brand-400 hover:underline">
          Practice
        </button>
        .
      </p>

      <Card className="space-y-5">
        <div>
          <label className="text-sm font-medium mb-2 block">Interview Type</label>
          <div className="grid grid-cols-2 gap-3">
            <button
              onClick={() => setType('adaptive')}
              className={`p-4 rounded-lg border text-left transition ${
                type === 'adaptive' ? 'border-brand-500 bg-brand-500/10' : 'border-white/10 hover:bg-white/5'
              }`}
            >
              <div className="font-semibold">Adaptive</div>
              <div className="text-xs text-gray-400 mt-1">Questions selected by your Elo across all topics.</div>
            </button>
            <button
              onClick={() => setType('topic')}
              className={`p-4 rounded-lg border text-left transition ${
                type === 'topic' ? 'border-brand-500 bg-brand-500/10' : 'border-white/10 hover:bg-white/5'
              }`}
            >
              <div className="font-semibold">Topic Focus</div>
              <div className="text-xs text-gray-400 mt-1">Practice a specific topic in interview format.</div>
            </button>
          </div>
        </div>

        {type === 'topic' && (
          <div>
            <label className="text-sm font-medium mb-2 block">Topic</label>
            <Select value={topic} onChange={(e) => setTopic(e.target.value)}>
              {topics.map((t) => (
                <option key={t} value={t}>{t}</option>
              ))}
            </Select>
          </div>
        )}

        <div>
          <label className="text-sm font-medium mb-2 block">Target Company (optional)</label>
          <Select value={targetCompanyId} onChange={(e) => setTargetCompanyId(e.target.value ? Number(e.target.value) : '')}>
            <option value="">None — general mock</option>
            {companies.map((c) => (
              <option key={c.id} value={c.id}>{c.logoEmoji} {c.name}</option>
            ))}
          </Select>
        </div>

        <div>
          <label className="text-sm font-medium mb-2 block">Target Role</label>
          <Select value={targetRole} onChange={(e) => setTargetRole(e.target.value)}>
            <option value="Software Engineer">Software Engineer</option>
            <option value="SDE Intern">SDE Intern</option>
            <option value="Senior Software Engineer">Senior Software Engineer</option>
            <option value="Full Stack Developer">Full Stack Developer</option>
          </Select>
        </div>

        <div>
          <label className="text-sm font-medium mb-2 block">Number of Questions: {numQuestions}</label>
          <input
            type="range"
            min={3}
            max={10}
            value={numQuestions}
            onChange={(e) => setNumQuestions(Number(e.target.value))}
            className="w-full accent-brand-500"
          />
        </div>

        <PrimaryButton onClick={handleStart} disabled={loading} className="w-full">
          {loading ? 'Starting…' : 'Start Interview'}
        </PrimaryButton>
      </Card>
    </div>
  );
}
