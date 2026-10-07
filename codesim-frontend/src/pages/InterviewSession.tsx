import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate, useParams, useBlocker } from 'react-router-dom';
import { api } from '../api/client';
import { Card, PrimaryButton, Badge, Spinner, Select } from '../components/ui';
import { ProblemAccordion } from '../components/ProblemAccordion';
import { CodeEditor, type EditorLang } from '../components/CodeEditor';
import { useAuthStore } from '../store/auth';
import { starterForLang } from '../utils/starterCode';
import type { NextQuestionResponse, SubmitResponse, RunTestResult } from '../types';

type Tab = 'output' | 'tests' | 'review';

const LANG_OPTIONS: { value: EditorLang; label: string }[] = [
  { value: 'python', label: 'Python' },
  { value: 'javascript', label: 'JavaScript' },
  { value: 'java', label: 'Java' },
  { value: 'cpp', label: 'C++' },
  { value: 'c', label: 'C' },
];

export default function InterviewSession() {
  const { id } = useParams();
  const navigate = useNavigate();
  const user = useAuthStore((s) => s.user);
  const updateUser = useAuthStore((s) => s.updateUser);

  const [state, setState] = useState<NextQuestionResponse | null>(null);
  const [code, setCode] = useState('');
  const [language, setLanguage] = useState<EditorLang>((user?.preferredLanguage as EditorLang) || 'python');
  const [loading, setLoading] = useState(true);
  const [running, setRunning] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [runResults, setRunResults] = useState<RunTestResult[] | null>(null);
  const [submitResult, setSubmitResult] = useState<SubmitResponse | null>(null);
  const [editingAfterSubmit, setEditingAfterSubmit] = useState(false);
  const [hint, setHint] = useState<string | null>(null);
  const [hintLoading, setHintLoading] = useState(false);
  const [tab, setTab] = useState<Tab>('tests');
  const [timeLeft, setTimeLeft] = useState(1800);
  const [startedAt, setStartedAt] = useState(Date.now());
  const [timerExpired, setTimerExpired] = useState(false);
  const [interviewActive, setInterviewActive] = useState(true);
  const timerRef = useRef<number | null>(null);
  const autoSubmittedRef = useRef(false);

  const editorLocked = !!submitResult && !editingAfterSubmit;
  const maxScore = submitResult?.maxScore ?? 10;

  const blocker = useBlocker(
    ({ currentLocation, nextLocation }) =>
      interviewActive &&
      !loading &&
      currentLocation.pathname !== nextLocation.pathname &&
      !nextLocation.pathname.endsWith('/results'),
  );

  const loadNext = useCallback(async () => {
    setLoading(true);
    setSubmitResult(null);
    setEditingAfterSubmit(false);
    setHint(null);
    setRunResults(null);
    setTimerExpired(false);
    autoSubmittedRef.current = false;
    setTab('tests');
    try {
      const res = await api.get<NextQuestionResponse>(`/interviews/${id}/next`);
      setState(res.data);
      if (res.data.completed) {
        setInterviewActive(false);
        await api.post(`/interviews/${id}/complete`);
        navigate(`/interview/${id}/results`);
        return;
      }
      const lang = (user?.preferredLanguage as EditorLang) || 'python';
      setCode(starterForLang(res.data.question, lang));
      setLanguage(lang);
      setTimeLeft(res.data.timeLimitSeconds ?? 1800);
      setStartedAt(Date.now());
    } finally {
      setLoading(false);
    }
  }, [id, navigate, user?.preferredLanguage]);

  useEffect(() => {
    loadNext();
  }, [loadNext]);

  useEffect(() => {
    if (loading || (submitResult && !editingAfterSubmit)) {
      if (timerRef.current) window.clearInterval(timerRef.current);
      return;
    }
    timerRef.current = window.setInterval(() => {
      setTimeLeft((t) => Math.max(0, t - 1));
    }, 1000);
    return () => {
      if (timerRef.current) window.clearInterval(timerRef.current);
    };
  }, [loading, submitResult, editingAfterSubmit]);

  const handleSubmit = useCallback(async () => {
    if (!state?.interviewQuestionId) return;
    setSubmitting(true);
    const timeTaken = Math.round((Date.now() - startedAt) / 1000);
    try {
      const res = await api.post<SubmitResponse>(`/interviews/questions/${state.interviewQuestionId}/submit`, {
        code,
        timeTaken,
        language,
      });
      setSubmitResult(res.data);
      setEditingAfterSubmit(false);
      setTab('review');
      const me = await api.get('/profile');
      updateUser(me.data.user);
    } finally {
      setSubmitting(false);
    }
  }, [state?.interviewQuestionId, startedAt, code, language, updateUser]);

  useEffect(() => {
    if (timeLeft === 0 && !submitResult && !loading && !autoSubmittedRef.current) {
      autoSubmittedRef.current = true;
      setTimerExpired(true);
      handleSubmit();
    }
  }, [timeLeft, submitResult, loading, handleSubmit]);

  useEffect(() => {
    if (blocker.state === 'blocked') {
      const leave = window.confirm('Leave interview? Your progress on this question will be saved but the session stays active.');
      if (leave) blocker.proceed();
      else blocker.reset();
    }
  }, [blocker]);

  function handleLanguageChange(lang: EditorLang) {
    setLanguage(lang);
    if (state?.question) {
      setCode(starterForLang(state.question, lang));
    }
  }

  function handleResetCode() {
    if (!state?.question) return;
    if (
      code !== starterForLang(state.question, language) &&
      !window.confirm('Reset to the default starter code? Your current code will be lost.')
    ) {
      return;
    }
    setCode(starterForLang(state.question, language));
    setEditingAfterSubmit(true);
  }

  async function handleRun() {
    if (!state?.question?.id) return;
    setRunning(true);
    setTab('output');
    try {
      const res = await api.post<{ results: RunTestResult[] }>('/interviews/run-tests', {
        code,
        language,
        questionId: state.question.id,
      });
      setRunResults(res.data.results);
    } finally {
      setRunning(false);
    }
  }

  async function handleHint() {
    if (!state?.interviewQuestionId) return;
    setHintLoading(true);
    try {
      const res = await api.post<{ hint: string }>(`/interviews/questions/${state.interviewQuestionId}/hint`, { code });
      setHint(res.data.hint);
    } finally {
      setHintLoading(false);
    }
  }

  async function handleSkip() {
    if (!state?.interviewQuestionId) return;
    await api.post(`/interviews/questions/${state.interviewQuestionId}/skip`);
    await loadNext();
  }

  function handleExitInterview() {
    if (window.confirm('Exit interview? You can resume later from your dashboard.')) {
      setInterviewActive(false);
      navigate('/dashboard');
    }
  }

  if (loading || !state?.question) {
    return (
      <div className="flex justify-center py-20">
        <Spinner />
      </div>
    );
  }

  const q = state.question;
  const minutes = Math.floor(timeLeft / 60);
  const seconds = timeLeft % 60;

  return (
    <div className="flex flex-col gap-3 -m-2" style={{ minHeight: 'calc(100vh - 4rem)' }}>
      <div className="flex items-center justify-between flex-wrap gap-2 px-1">
        <div className="flex items-center gap-3">
          <div className="text-sm text-gray-400">
            Question {state.questionNumber} / {state.totalQuestions}
          </div>
          <button
            onClick={handleExitInterview}
            className="text-xs px-2 py-1 rounded border border-white/10 text-gray-400 hover:text-red-400 hover:border-red-500/30"
          >
            Exit Interview
          </button>
        </div>
        <div className="flex items-center gap-3">
          <Select value={language} onChange={(e) => handleLanguageChange(e.target.value as EditorLang)} className="text-sm">
            {LANG_OPTIONS.map((l) => (
              <option key={l.value} value={l.value}>{l.label}</option>
            ))}
          </Select>
          <button
            type="button"
            onClick={handleResetCode}
            disabled={editorLocked}
            title="Reset to default code"
            className="text-xs px-2.5 py-1.5 rounded-lg border border-white/10 text-gray-400 hover:text-gray-200 hover:bg-white/5 disabled:opacity-50"
          >
            Reset Code
          </button>
          <div className={`font-mono font-bold text-lg ${timeLeft < 120 ? 'text-red-400' : 'text-gray-200'}`}>
            {minutes}:{seconds.toString().padStart(2, '0')}
          </div>
        </div>
      </div>

      {timerExpired && submitResult && (
        <div className="mx-1 px-4 py-2 rounded-lg bg-amber-500/15 border border-amber-500/30 text-amber-200 text-sm">
          Time&apos;s up — your solution was auto-submitted.
        </div>
      )}

      <ProblemAccordion
        title={q.title}
        difficulty={q.difficulty}
        topic={q.topic}
        description={q.description}
        examples={q.examples}
        constraints={q.constraints}
      >
        <div className="mt-3 flex items-center gap-2">
          <button
            onClick={handleHint}
            disabled={hintLoading || editorLocked}
            className="text-xs px-3 py-1.5 rounded-lg border border-white/10 hover:bg-white/5 disabled:opacity-50"
          >
            💡 {hintLoading ? 'Thinking…' : 'Get a Hint'}
          </button>
          <button
            onClick={handleSkip}
            disabled={editorLocked}
            className="text-xs px-3 py-1.5 rounded-lg border border-white/10 hover:bg-white/5 disabled:opacity-50"
          >
            Skip →
          </button>
        </div>
        {hint && <div className="mt-2 text-sm bg-brand-500/10 border border-brand-500/30 rounded-lg p-3">{hint}</div>}
      </ProblemAccordion>

      <Card className="mx-1 flex-1 p-0 overflow-hidden min-h-[45vh]">
        <CodeEditor
          value={code}
          onChange={setCode}
          language={language}
          height="calc(55vh - 2rem)"
          readOnly={editorLocked}
        />
      </Card>

      <div className="flex gap-2 px-1">
        <button
          onClick={handleRun}
          disabled={running || editorLocked}
          className="flex-1 glass-card py-2.5 rounded-lg font-semibold text-sm hover:bg-white/5 disabled:opacity-50"
        >
          {running ? 'Running…' : '▶ Run'}
        </button>
        {submitResult && !editingAfterSubmit ? (
          <PrimaryButton onClick={() => setEditingAfterSubmit(true)} className="flex-1">
            Edit & Resubmit
          </PrimaryButton>
        ) : (
          <PrimaryButton onClick={handleSubmit} disabled={submitting} className="flex-1">
            {submitting ? 'Submitting…' : submitResult ? 'Resubmit' : 'Submit'}
          </PrimaryButton>
        )}
      </div>

      <Card className="mx-1 min-h-[280px]">
        <div className="flex gap-2 mb-3 text-xs">
          {(['output', 'tests', 'review'] as Tab[]).map((t) => (
            <button
              key={t}
              onClick={() => setTab(t)}
              className={`px-3 py-1.5 rounded-full capitalize ${tab === t ? 'bg-brand-500/20 text-brand-300' : 'text-gray-400 hover:bg-white/5'}`}
            >
              {t}
            </button>
          ))}
        </div>

        {tab === 'output' && (
          <div className="space-y-2">
            {!runResults && <p className="text-sm text-gray-500">Run your code to see output for all sample test cases.</p>}
            {runResults?.map((r) => (
              <div key={r.testNum} className={`text-sm rounded-lg p-2.5 border ${r.passed ? 'border-emerald-500/30 bg-emerald-500/5' : 'border-red-500/30 bg-red-500/5'}`}>
                <div className="font-medium">
                  Test Case {r.testNum} — {r.passed ? '✅ Passed' : '❌ Failed'}
                  {r.description && <span className="text-gray-400 font-normal ml-2">{r.description}</span>}
                </div>
                {r.stdout && <pre className="text-xs font-mono mt-1 text-gray-300 whitespace-pre-wrap">{r.stdout}</pre>}
                {!r.passed && r.error && <div className="text-xs text-red-300 mt-1">{r.error}</div>}
              </div>
            ))}
          </div>
        )}

        {tab === 'tests' && (
          <div className="space-y-2">
            {!submitResult && <p className="text-sm text-gray-500">Submit to run against all test cases (including hidden).</p>}
            {submitResult?.testResults.filter((r) => !r.hidden).map((r) => (
              <div key={r.testNum} className={`text-sm rounded-lg p-2.5 border ${r.passed ? 'border-emerald-500/30 bg-emerald-500/5' : 'border-red-500/30 bg-red-500/5'}`}>
                <div className="flex justify-between font-medium">
                  <span>Test Case {r.testNum} — {r.passed ? '✅ Passed' : '❌ Failed'}</span>
                </div>
                {!r.passed && r.error && <div className="text-xs text-red-300 mt-1">{r.error}</div>}
              </div>
            ))}
            {submitResult && submitResult.testResults.some((r) => r.hidden) && (
              <>
                <div className="text-xs uppercase text-gray-500 font-semibold pt-2">Hidden Test Cases</div>
                {submitResult.testResults.filter((r) => r.hidden).map((r) => (
                  <div key={r.testNum} className={`text-sm rounded-lg p-2.5 border ${r.passed ? 'border-emerald-500/30 bg-emerald-500/5' : 'border-red-500/30 bg-red-500/5'}`}>
                    <div className="flex justify-between font-medium">
                      <span>Hidden Case {r.testNum} — {r.passed ? '✅ Passed' : '❌ Failed'}</span>
                      <Badge tone="gray">hidden</Badge>
                    </div>
                    {!r.passed && r.error && <div className="text-xs text-red-300 mt-1">{r.error}</div>}
                  </div>
                ))}
              </>
            )}
            {submitResult && (
              <div className="pt-2 text-sm font-semibold">
                Score: {submitResult.score.toFixed(1)}/{maxScore} · Elo {submitResult.eloChange >= 0 ? '+' : ''}{submitResult.eloChange}
              </div>
            )}
          </div>
        )}

        {tab === 'review' && (
          <div className="text-sm space-y-2 max-h-48 overflow-y-auto">
            {!submitResult && <p className="text-gray-500">Submit your code to get an AI review.</p>}
            {submitResult && (
              <>
                <p className="text-gray-300">{submitResult.review.overall_assessment}</p>
                <div className="text-xs text-gray-400 flex gap-4">
                  <span>Time: {submitResult.review.time_complexity}</span>
                  <span>Space: {submitResult.review.space_complexity}</span>
                </div>
                {submitResult.review.optimized_approach && (
                  <p className="text-xs text-gray-400"><strong>Optimal:</strong> {submitResult.review.optimized_approach}</p>
                )}
                <div>
                  <div className="text-xs uppercase text-gray-500 font-semibold mt-2">Strengths</div>
                  <ul className="list-disc list-inside text-gray-300">
                    {submitResult.review.strengths?.map((s, i) => <li key={i}>{s}</li>)}
                  </ul>
                </div>
                <div>
                  <div className="text-xs uppercase text-gray-500 font-semibold mt-2">Improvements</div>
                  <ul className="list-disc list-inside text-gray-300">
                    {submitResult.review.improvements?.map((s, i) => <li key={i}>{s}</li>)}
                  </ul>
                </div>
                <div className="text-xs text-gray-400 pt-2">💡 {submitResult.review.interview_tip}</div>
              </>
            )}
          </div>
        )}
      </Card>

      {submitResult && !editingAfterSubmit && (
        <PrimaryButton onClick={loadNext} className="mx-1 w-auto">
          {(state.questionNumber ?? 0) < (state.totalQuestions ?? 0) ? 'Next Question →' : 'Finish Interview →'}
        </PrimaryButton>
      )}
    </div>
  );
}
