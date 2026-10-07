import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import CodeMirror from '@uiw/react-codemirror';
import { python } from '@codemirror/lang-python';
import { json } from '@codemirror/lang-json';
import { oneDark } from '@codemirror/theme-one-dark';
import { api, errorMessage } from '../../api/client';
import { Card, Input, Select, PrimaryButton } from '../../components/ui';

interface AdminQuestionDetail {
  title: string;
  description: string;
  difficulty: string;
  topic: string;
  subtopic: string;
  starterCode: string;
  solutionCode: string;
  functionSignature: string;
  testCases: string;
  hints: string;
  constraints: string;
  examples: string;
  timeComplexity: string;
  spaceComplexity: string;
  tags: string;
}

const emptyForm = {
  title: '',
  description: '',
  difficulty: 'medium',
  topic: '',
  subtopic: '',
  starterCode: 'def solution():\n    pass\n',
  solutionCode: '',
  functionSignature: '',
  testCases: '[\n  {"args": [], "expected_output": null, "description": "Example"}\n]',
  hints: '[]',
  constraints: '',
  examples: '',
  timeComplexity: '',
  spaceComplexity: '',
  tags: '',
};

export default function AdminQuestionForm() {
  const { id } = useParams();
  const isEdit = !!id;
  const navigate = useNavigate();
  const [form, setForm] = useState(emptyForm);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!isEdit) return;
    api.get<AdminQuestionDetail>(`/admin/questions/${id}`).then((res) => {
      const q = res.data;
      setForm({
        title: q.title,
        description: q.description,
        difficulty: q.difficulty,
        topic: q.topic,
        subtopic: q.subtopic,
        starterCode: q.starterCode,
        solutionCode: q.solutionCode,
        functionSignature: q.functionSignature,
        testCases: JSON.stringify(JSON.parse(q.testCases || '[]'), null, 2),
        hints: JSON.stringify(JSON.parse(q.hints || '[]'), null, 2),
        constraints: q.constraints,
        examples: q.examples,
        timeComplexity: q.timeComplexity,
        spaceComplexity: q.spaceComplexity,
        tags: q.tags,
      });
    }).catch(() => {});
  }, [id, isEdit]);

  function set<K extends keyof typeof form>(key: K, value: string) {
    setForm((f) => ({ ...f, [key]: value }));
  }

  async function handleSave() {
    setError('');
    try {
      const cases = JSON.parse(form.testCases || '[]') as { hidden?: boolean }[];
      const hiddenCount = cases.filter((c) => c.hidden).length;
      if (hiddenCount < 3 && !window.confirm(`Only ${hiddenCount} hidden test case(s). Recommend at least 3 for robust validation. Save anyway?`)) {
        return;
      }
    } catch {
      setError('Invalid test cases JSON');
      return;
    }
    setSaving(true);
    try {
      if (isEdit) {
        await api.put(`/admin/questions/${id}`, form);
      } else {
        await api.post('/admin/questions', form);
      }
      navigate('/admin/questions');
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="max-w-4xl space-y-4">
      <h1 className="text-2xl font-bold">{isEdit ? 'Edit Question' : 'New Question'}</h1>

      {error && <div className="bg-red-500/10 border border-red-500/30 text-red-300 text-sm rounded-lg px-3 py-2">{error}</div>}

      <Card className="space-y-4">
        <div className="grid md:grid-cols-2 gap-3">
          <Input placeholder="Title" value={form.title} onChange={(e) => set('title', e.target.value)} />
          <Input placeholder="Topic (e.g. Arrays)" value={form.topic} onChange={(e) => set('topic', e.target.value)} />
          <Input placeholder="Subtopic" value={form.subtopic} onChange={(e) => set('subtopic', e.target.value)} />
          <Select value={form.difficulty} onChange={(e) => set('difficulty', e.target.value)}>
            <option value="easy">Easy</option>
            <option value="medium">Medium</option>
            <option value="hard">Hard</option>
            <option value="expert">Expert</option>
          </Select>
          <Input placeholder="Time Complexity" value={form.timeComplexity} onChange={(e) => set('timeComplexity', e.target.value)} />
          <Input placeholder="Space Complexity" value={form.spaceComplexity} onChange={(e) => set('spaceComplexity', e.target.value)} />
          <Input placeholder="Tags (comma separated)" value={form.tags} onChange={(e) => set('tags', e.target.value)} className="md:col-span-2" />
        </div>

        <textarea
          placeholder="Description"
          value={form.description}
          onChange={(e) => set('description', e.target.value)}
          rows={3}
          className="w-full bg-white/5 border border-white/10 rounded-lg px-3 py-2.5 text-sm focus:outline-none focus:border-brand-500"
        />
        <textarea
          placeholder="Examples"
          value={form.examples}
          onChange={(e) => set('examples', e.target.value)}
          rows={3}
          className="w-full bg-white/5 border border-white/10 rounded-lg px-3 py-2.5 text-sm font-mono focus:outline-none focus:border-brand-500"
        />
        <textarea
          placeholder="Constraints"
          value={form.constraints}
          onChange={(e) => set('constraints', e.target.value)}
          rows={2}
          className="w-full bg-white/5 border border-white/10 rounded-lg px-3 py-2.5 text-sm font-mono focus:outline-none focus:border-brand-500"
        />

        <div>
          <label className="text-xs text-gray-400 mb-1 block">Starter Code</label>
          <div className="rounded-lg overflow-hidden border border-white/10">
            <CodeMirror value={form.starterCode} height="140px" theme={oneDark} extensions={[python()]} onChange={(v) => set('starterCode', v)} />
          </div>
        </div>

        <div>
          <label className="text-xs text-gray-400 mb-1 block">Solution Code (used for reference; not shown to users)</label>
          <div className="rounded-lg overflow-hidden border border-white/10">
            <CodeMirror value={form.solutionCode} height="140px" theme={oneDark} extensions={[python()]} onChange={(v) => set('solutionCode', v)} />
          </div>
        </div>

        <div>
          <label className="text-xs text-gray-400 mb-1 block">
            Test Cases (JSON array — each: {'{'}"args": [...], "expected_output": ..., "description": "", "hidden": false{'}'})
          </label>
          <div className="rounded-lg overflow-hidden border border-white/10">
            <CodeMirror value={form.testCases} height="160px" theme={oneDark} extensions={[json()]} onChange={(v) => set('testCases', v)} />
          </div>
        </div>

        <div>
          <label className="text-xs text-gray-400 mb-1 block">Hints (JSON array of strings)</label>
          <div className="rounded-lg overflow-hidden border border-white/10">
            <CodeMirror value={form.hints} height="100px" theme={oneDark} extensions={[json()]} onChange={(v) => set('hints', v)} />
          </div>
        </div>

        <PrimaryButton onClick={handleSave} disabled={saving}>{saving ? 'Saving…' : 'Save Question'}</PrimaryButton>
      </Card>
    </div>
  );
}
