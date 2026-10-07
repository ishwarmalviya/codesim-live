import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { Card, PrimaryButton, Spinner } from '../components/ui';
import type { InterviewDto } from '../types';

export default function AssessmentSetup() {
  const navigate = useNavigate();
  const [topics, setTopics] = useState<string[]>([]);
  const [selectedTopics, setSelectedTopics] = useState<string[]>([]);
  const [numQuestions, setNumQuestions] = useState(6);
  const [loading, setLoading] = useState(false);
  const [step, setStep] = useState(1);

  useEffect(() => {
    api.get<string[]>('/interviews/topics').then((res) => setTopics(res.data));
  }, []);

  function toggleTopic(topic: string) {
    setSelectedTopics((prev) =>
      prev.includes(topic) ? prev.filter((t) => t !== topic) : [...prev, topic]
    );
  }

  async function handleStart() {
    setLoading(true);
    try {
      const res = await api.post<InterviewDto>('/interviews/start', {
        type: 'assessment',
        topic: selectedTopics.length === 1 ? selectedTopics[0] : 'adaptive',
        topics: selectedTopics.length > 0 ? selectedTopics : topics,
        numQuestions,
      });
      navigate(`/interview/${res.data.id}`);
    } finally {
      setLoading(false);
    }
  }

  if (topics.length === 0) {
    return <div className="flex justify-center py-20"><Spinner /></div>;
  }

  return (
    <div className="max-w-xl mx-auto space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Initial Assessment Setup</h1>
        <p className="text-gray-400 text-sm mt-1">Step {step} of 2 — configure your diagnostic interview</p>
      </div>

      {step === 1 && (
        <Card className="space-y-4">
          <h3 className="font-semibold">Select Topics</h3>
          <p className="text-sm text-gray-400">Choose topics to assess, or leave all selected for a full diagnostic.</p>
          <div className="flex flex-wrap gap-2">
            <button
              onClick={() => setSelectedTopics(selectedTopics.length === topics.length ? [] : [...topics])}
              className="text-xs px-3 py-1.5 rounded-full border border-white/10 hover:bg-white/5"
            >
              {selectedTopics.length === topics.length ? 'Deselect All' : 'Select All'}
            </button>
            {topics.map((t) => (
              <button
                key={t}
                onClick={() => toggleTopic(t)}
                className={`text-xs px-3 py-1.5 rounded-full border transition ${
                  selectedTopics.includes(t) || selectedTopics.length === 0
                    ? 'border-brand-500 bg-brand-500/15 text-brand-300'
                    : 'border-white/10 text-gray-400 hover:bg-white/5'
                }`}
              >
                {t}
              </button>
            ))}
          </div>
          <PrimaryButton onClick={() => setStep(2)} className="w-full">Next: Question Count →</PrimaryButton>
        </Card>
      )}

      {step === 2 && (
        <Card className="space-y-5">
          <h3 className="font-semibold">Number of Questions: {numQuestions}</h3>
          <input
            type="range"
            min={3}
            max={10}
            value={numQuestions}
            onChange={(e) => setNumQuestions(Number(e.target.value))}
            className="w-full accent-brand-500"
          />
          <p className="text-sm text-gray-400">
            Topics: {selectedTopics.length > 0 ? selectedTopics.join(', ') : 'All topics'}
          </p>
          <div className="flex gap-2">
            <button onClick={() => setStep(1)} className="flex-1 glass-card py-2.5 rounded-lg font-semibold text-sm hover:bg-white/5">
              ← Back
            </button>
            <PrimaryButton onClick={handleStart} disabled={loading} className="flex-1">
              {loading ? 'Starting…' : 'Start Assessment'}
            </PrimaryButton>
          </div>
        </Card>
      )}
    </div>
  );
}
