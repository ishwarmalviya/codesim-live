import { useEffect, useState } from 'react';
import { api } from '../api/client';
import { Card, PrimaryButton, Badge, Spinner } from '../components/ui';
import type { ResumeDto } from '../types';

export default function Resume() {
  const [resume, setResume] = useState<ResumeDto | null | undefined>(undefined);
  const [text, setText] = useState('');
  const [saving, setSaving] = useState(false);
  const [pdfBusy, setPdfBusy] = useState(false);

  function applyResume(data: ResumeDto | null | undefined) {
    if (!data || typeof data !== 'object') {
      setResume(null);
      setText('');
      return;
    }
    setResume({
      ...data,
      parsedSkills: data.parsedSkills ?? [],
    });
    setText(data.rawText ?? '');
  }

  function load() {
    api.get<ResumeDto>('/resume', { validateStatus: (s) => s === 200 || s === 204 })
      .then((res) => {
        if (res.status === 204) {
          applyResume(null);
          return;
        }
        applyResume(res.data);
      })
      .catch(() => applyResume(null));
  }

  useEffect(load, []);

  async function handleSave() {
    setSaving(true);
    try {
      const res = await api.post<ResumeDto>('/resume', { rawText: text });
      applyResume(res.data);
    } finally {
      setSaving(false);
    }
  }

  async function handlePdfUpload(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;
    setPdfBusy(true);
    try {
      const form = new FormData();
      form.append('file', file);
      const res = await api.post<ResumeDto>('/resume/upload', form, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      applyResume(res.data);
    } finally {
      setPdfBusy(false);
      e.target.value = '';
    }
  }

  if (resume === undefined) {
    return <div className="flex justify-center py-20"><Spinner /></div>;
  }

  const analysis = resume?.analysis;
  const skills = resume?.parsedSkills ?? [];
  const tips = analysis?.companySpecificTips;
  const tipEntries = tips && typeof tips === 'object' ? Object.entries(tips) : [];

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Resume Analyzer</h1>
        <p className="text-gray-400 text-sm mt-1">
          Upload a PDF or paste text for AI-powered ATS score, missing skills, and company-specific tips.
        </p>
      </div>

      <Card className="space-y-4">
        <div>
          <label className="text-sm font-medium mb-2 block">Upload PDF Resume</label>
          <input
            type="file"
            accept=".pdf,application/pdf"
            onChange={handlePdfUpload}
            disabled={pdfBusy}
            className="text-sm text-gray-400 file:mr-4 file:py-2 file:px-4 file:rounded-lg file:border-0 file:bg-brand-500/20 file:text-brand-300"
          />
          {pdfBusy && <p className="text-xs text-gray-500 mt-2">Parsing PDF…</p>}
        </div>
        <div className="text-xs text-gray-500 text-center">— or paste text below —</div>
        <textarea
          value={text}
          onChange={(e) => setText(e.target.value)}
          rows={10}
          placeholder="Paste your resume text here…"
          className="w-full bg-white/5 border border-white/10 rounded-lg px-3 py-2.5 text-sm focus:outline-none focus:border-brand-500"
        />
        <PrimaryButton onClick={handleSave} disabled={saving || !(text ?? '').trim()}>
          {saving ? 'Analyzing…' : 'Analyze Resume'}
        </PrimaryButton>
      </Card>

      {resume && (
        <>
          <Card>
            <h3 className="font-semibold mb-3">Profile Summary</h3>
            <div className="flex flex-wrap gap-3 mb-3">
              <Badge tone="brand">{resume.experienceLevel}</Badge>
              {resume.atsScore != null && (
                <Badge tone={resume.atsScore >= 70 ? 'green' : resume.atsScore >= 50 ? 'yellow' : 'red'}>
                  ATS Score: {resume.atsScore}/100
                </Badge>
              )}
              {analysis?.yearsOfExperience != null && (
                <Badge tone="gray">{analysis.yearsOfExperience} years experience</Badge>
              )}
            </div>
            {analysis?.summary && <p className="text-sm text-gray-300">{analysis.summary}</p>}
          </Card>

          <Card>
            <h3 className="font-semibold mb-3">Technical Skills</h3>
            <div className="flex flex-wrap gap-2">
              {skills.map((s) => (
                <Badge key={s} tone="brand">{s}</Badge>
              ))}
              {skills.length === 0 && <p className="text-sm text-gray-500">No skills detected.</p>}
            </div>
          </Card>

          {analysis?.missingSkills && analysis.missingSkills.length > 0 && (
            <Card>
              <h3 className="font-semibold mb-3">Missing Skills</h3>
              <div className="flex flex-wrap gap-2">
                {analysis.missingSkills.map((s) => (
                  <Badge key={s} tone="red">{s}</Badge>
                ))}
              </div>
            </Card>
          )}

          {analysis?.improvements && analysis.improvements.length > 0 && (
            <Card>
              <h3 className="font-semibold mb-3">Improvement Suggestions</h3>
              <ul className="list-disc list-inside text-sm text-gray-300 space-y-1">
                {analysis.improvements.map((s, i) => <li key={i}>{s}</li>)}
              </ul>
            </Card>
          )}

          {tipEntries.length > 0 && (
            <Card>
              <h3 className="font-semibold mb-3">Company-Specific Tips</h3>
              {tipEntries.map(([company, tip]) => (
                <div key={company} className="text-sm mb-2">
                  <span className="font-medium text-gray-200">{company}:</span>{' '}
                  <span className="text-gray-400">{String(tip ?? '')}</span>
                </div>
              ))}
            </Card>
          )}
        </>
      )}
    </div>
  );
}
