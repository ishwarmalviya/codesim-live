import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { Card, PrimaryButton, Select, Spinner } from '../components/ui';
import type { CompanyDto } from '../types';

export default function RoadmapSetup() {
  const navigate = useNavigate();
  const [companies, setCompanies] = useState<CompanyDto[]>([]);
  const [skillLevel, setSkillLevel] = useState('Intermediate');
  const [placementGoal, setPlacementGoal] = useState('full-time');
  const [selectedCompanies, setSelectedCompanies] = useState<number[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    api.get<CompanyDto[]>('/companies').then((res) => setCompanies(res.data));
  }, []);

  async function handleGenerate() {
    setLoading(true);
    try {
      await api.post('/roadmap/generate', {
        skillLevel,
        placementGoal,
        targetCompanyIds: selectedCompanies,
        useSavedResume: true,
      });
      navigate('/roadmap');
    } finally {
      setLoading(false);
    }
  }

  if (companies.length === 0) {
    return <div className="flex justify-center py-20"><Spinner /></div>;
  }

  return (
    <div className="max-w-xl mx-auto space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Personalized Roadmap Setup</h1>
        <p className="text-gray-400 text-sm mt-1">Tell us about your goals — AI will build a 4-week study plan.</p>
      </div>

      <Card className="space-y-4">
        <div>
          <label className="text-sm font-medium mb-2 block">Current Skill Level</label>
          <Select value={skillLevel} onChange={(e) => setSkillLevel(e.target.value)}>
            <option value="Beginner">Beginner</option>
            <option value="Intermediate">Intermediate</option>
            <option value="Advanced">Advanced</option>
          </Select>
        </div>

        <div>
          <label className="text-sm font-medium mb-2 block">Placement Goal</label>
          <Select value={placementGoal} onChange={(e) => setPlacementGoal(e.target.value)}>
            <option value="internship">Internship</option>
            <option value="full-time">Full-time</option>
            <option value="6-months">Within 6 months</option>
            <option value="1-year">Within 1 year</option>
          </Select>
        </div>

        <div>
          <label className="text-sm font-medium mb-2 block">Target Companies</label>
          <div className="flex flex-wrap gap-2">
            {companies.map((c) => (
              <button
                key={c.id}
                onClick={() => setSelectedCompanies((prev) =>
                  prev.includes(c.id) ? prev.filter((id) => id !== c.id) : [...prev, c.id]
                )}
                className={`text-xs px-3 py-1.5 rounded-full border ${
                  selectedCompanies.includes(c.id)
                    ? 'border-brand-500 bg-brand-500/15 text-brand-300'
                    : 'border-white/10 text-gray-400'
                }`}
              >
                {c.logoEmoji} {c.name}
              </button>
            ))}
          </div>
        </div>

        <p className="text-xs text-gray-500">
          Uses your saved resume from the <Link to="/resume" className="text-brand-400">Resume Analyzer</Link>.
        </p>

        <PrimaryButton onClick={handleGenerate} disabled={loading} className="w-full">
          {loading ? 'Generating…' : 'Generate AI Roadmap'}
        </PrimaryButton>
      </Card>
    </div>
  );
}
