import { useEffect, useState } from 'react';
import { api } from '../../api/client';
import { Card, Input, Badge, Spinner } from '../../components/ui';

interface AdminUserDto {
  id: number;
  name: string;
  email: string;
  college: string;
  eloRating: number;
  skillLevel: string;
  totalInterviews: number;
  avgScore: number;
  createdAt: string;
  isActiveAccount: boolean;
}

interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
}

export default function AdminUsers() {
  const [page, setPage] = useState<Page<AdminUserDto> | null>(null);
  const [search, setSearch] = useState('');

  function load(searchTerm: string, pageNum: number) {
    api.get<Page<AdminUserDto>>('/admin/users', { params: { search: searchTerm, page: pageNum } }).then((res) => setPage(res.data));
  }

  useEffect(() => load('', 1), []);

  async function toggle(id: number) {
    await api.post(`/admin/users/${id}/toggle`);
    load(search, (page?.number ?? 0) + 1);
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between flex-wrap gap-3">
        <h1 className="text-2xl font-bold">Users</h1>
        <div className="w-64">
          <Input
            placeholder="Search name or email…"
            value={search}
            onChange={(e) => {
              setSearch(e.target.value);
              load(e.target.value, 1);
            }}
          />
        </div>
      </div>

      <Card>
        {!page ? (
          <div className="flex justify-center py-10"><Spinner /></div>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="text-gray-400 text-left border-b border-white/10">
                <th className="pb-2">Name</th>
                <th className="pb-2">Email</th>
                <th className="pb-2">Elo</th>
                <th className="pb-2">Interviews</th>
                <th className="pb-2">Status</th>
                <th className="pb-2"></th>
              </tr>
            </thead>
            <tbody>
              {page.content.map((u) => (
                <tr key={u.id} className="border-b border-white/5">
                  <td className="py-2">{u.name}</td>
                  <td className="py-2 text-gray-400">{u.email}</td>
                  <td className="py-2">{Math.round(u.eloRating)} <span className="text-gray-500">({u.skillLevel})</span></td>
                  <td className="py-2">{u.totalInterviews}</td>
                  <td className="py-2">
                    <Badge tone={u.isActiveAccount ? 'green' : 'red'}>{u.isActiveAccount ? 'active' : 'suspended'}</Badge>
                  </td>
                  <td className="py-2 text-right">
                    <button onClick={() => toggle(u.id)} className="text-xs text-brand-400 hover:text-brand-300">
                      {u.isActiveAccount ? 'Suspend' : 'Activate'}
                    </button>
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
