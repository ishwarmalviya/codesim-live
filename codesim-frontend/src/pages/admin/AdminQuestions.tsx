import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../../api/client';
import { Card, Input, Select, Badge, Spinner } from '../../components/ui';
import type { QuestionDto } from '../../types';

interface Page<T> {
  content: T[];
  totalElements: number;
}

export default function AdminQuestions() {
  const [page, setPage] = useState<Page<QuestionDto> | null>(null);
  const [search, setSearch] = useState('');
  const [difficulty, setDifficulty] = useState('');

  function load(s: string, d: string) {
    api.get<Page<QuestionDto>>('/admin/questions', { params: { search: s, difficulty: d, page: 1 } }).then((res) => setPage(res.data));
  }

  useEffect(() => load('', ''), []);

  async function deactivate(id: number) {
    if (!confirm('Deactivate this question?')) return;
    await api.delete(`/admin/questions/${id}`);
    load(search, difficulty);
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between flex-wrap gap-3">
        <h1 className="text-2xl font-bold">Question Bank</h1>
        <Link to="/admin/questions/new" className="btn-primary text-white font-semibold px-4 py-2 rounded-lg text-sm">
          + New Question
        </Link>
      </div>

      <div className="flex gap-3">
        <div className="flex-1">
          <Input placeholder="Search title…" value={search} onChange={(e) => { setSearch(e.target.value); load(e.target.value, difficulty); }} />
        </div>
        <div className="w-40">
          <Select value={difficulty} onChange={(e) => { setDifficulty(e.target.value); load(search, e.target.value); }}>
            <option value="">All Difficulty</option>
            <option value="easy">Easy</option>
            <option value="medium">Medium</option>
            <option value="hard">Hard</option>
            <option value="expert">Expert</option>
          </Select>
        </div>
      </div>

      <Card>
        {!page ? (
          <div className="flex justify-center py-10"><Spinner /></div>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="text-gray-400 text-left border-b border-white/10">
                <th className="pb-2">Title</th>
                <th className="pb-2">Topic</th>
                <th className="pb-2">Difficulty</th>
                <th className="pb-2">Success Rate</th>
                <th className="pb-2"></th>
              </tr>
            </thead>
            <tbody>
              {page.content.map((q) => (
                <tr key={q.id} className="border-b border-white/5">
                  <td className="py-2">{q.title}</td>
                  <td className="py-2 text-gray-400">{q.topic}</td>
                  <td className="py-2">
                    <Badge tone={q.difficulty === 'easy' ? 'green' : q.difficulty === 'medium' ? 'yellow' : 'red'}>{q.difficulty}</Badge>
                  </td>
                  <td className="py-2">{q.successRate}%</td>
                  <td className="py-2 text-right space-x-3">
                    <Link to={`/admin/questions/${q.id}/edit`} className="text-brand-400 hover:text-brand-300 text-xs">Edit</Link>
                    <button onClick={() => deactivate(q.id)} className="text-red-400 hover:text-red-300 text-xs">Deactivate</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>
    </div>
  );
}
