import { useEffect, useState } from 'react';
import { api } from '../../api/client';
import { Card, Spinner, PrimaryButton, Select } from '../../components/ui';
import type { QuestionDto } from '../../types';

interface ContactMessage {
  id: number;
  name: string;
  email: string;
  message: string;
  messageType: string;
  isRead: boolean;
  createdAt: string;
}

export default function AdminMessages() {
  const [messages, setMessages] = useState<ContactMessage[]>([]);
  const [loading, setLoading] = useState(true);

  function load() {
    api.get<ContactMessage[]>('/admin/messages').then((res) => {
      setMessages(res.data as ContactMessage[]);
      setLoading(false);
    });
  }

  useEffect(load, []);

  async function markRead(id: number) {
    await api.patch(`/admin/messages/${id}/read`);
    load();
  }

  if (loading) return <div className="flex justify-center py-20"><Spinner /></div>;

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Contact Messages</h1>
      {messages.length === 0 ? (
        <Card className="text-gray-400 text-center py-8">No messages yet.</Card>
      ) : (
        messages.map((m) => (
          <Card key={m.id} className={m.isRead ? 'opacity-70' : ''}>
            <div className="flex justify-between items-start gap-4">
              <div>
                <div className="font-semibold">{m.name} <span className="text-gray-500 font-normal">&lt;{m.email}&gt;</span></div>
                <div className="text-xs text-gray-500 mt-1">
                  {m.messageType} · {new Date(m.createdAt).toLocaleString()}
                </div>
                <p className="text-sm text-gray-300 mt-2 whitespace-pre-wrap">{m.message}</p>
              </div>
              {!m.isRead && (
                <button onClick={() => markRead(m.id)} className="text-xs text-brand-400 hover:underline shrink-0">
                  Mark read
                </button>
              )}
            </div>
          </Card>
        ))
      )}
    </div>
  );
}

interface Page<T> { content: T[] }

export function AdminDailyQuestionPicker() {
  const [questions, setQuestions] = useState<QuestionDto[]>([]);
  const [selectedId, setSelectedId] = useState<number | ''>('');
  const [assigning, setAssigning] = useState(false);
  const [message, setMessage] = useState('');

  useEffect(() => {
    api.get<Page<QuestionDto>>('/admin/questions?page=1').then((res) => setQuestions(res.data.content ?? []));
  }, []);

  async function assign() {
    if (!selectedId) return;
    setAssigning(true);
    try {
      await api.post('/admin/daily-question', { questionId: selectedId });
      setMessage('Daily question assigned! Expires at 5:00 AM IST.');
    } catch {
      setMessage('Failed to assign daily question.');
    } finally {
      setAssigning(false);
    }
  }

  return (
    <Card className="space-y-4">
      <h3 className="font-semibold">Daily Question</h3>
      <p className="text-sm text-gray-400">Assign a question as today&apos;s daily challenge. Resets at 5:00 AM IST.</p>
      <Select value={selectedId} onChange={(e) => setSelectedId(e.target.value ? Number(e.target.value) : '')}>
        <option value="">Select a question…</option>
        {questions.map((q) => (
          <option key={q.id} value={q.id}>{q.title} ({q.topic})</option>
        ))}
      </Select>
      <PrimaryButton onClick={assign} disabled={assigning || !selectedId}>
        {assigning ? 'Assigning…' : 'Set as Daily Question'}
      </PrimaryButton>
      {message && <p className="text-sm text-brand-300">{message}</p>}
    </Card>
  );
}
