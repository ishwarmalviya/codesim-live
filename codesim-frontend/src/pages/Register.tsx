import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api, errorMessage } from '../api/client';
import { useAuthStore } from '../store/auth';
import { PrimaryButton, Input } from '../components/ui';
import type { UserDto } from '../types';

export default function Register() {
  const navigate = useNavigate();
  const setAuth = useAuthStore((s) => s.setAuth);
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [college, setCollege] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = await api.post<{ token: string; user: UserDto }>('/auth/register', { name, email, college, password });
      setAuth(res.data.token, res.data.user);
      navigate('/dashboard');
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center px-4 py-10">
      <div className="glass-card p-8 w-full max-w-md">
        <div className="text-center mb-6">
          <span className="text-3xl">🧠</span>
          <h1 className="text-2xl font-bold mt-2">Create your account</h1>
          <p className="text-gray-400 text-sm mt-1">Start your adaptive interview journey</p>
        </div>

        {error && <div className="bg-red-500/10 border border-red-500/30 text-red-300 text-sm rounded-lg px-3 py-2 mb-4">{error}</div>}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="text-xs text-gray-400 mb-1 block">Full name</label>
            <Input required value={name} onChange={(e) => setName(e.target.value)} placeholder="Jane Doe" />
          </div>
          <div>
            <label className="text-xs text-gray-400 mb-1 block">Email</label>
            <Input type="email" required value={email} onChange={(e) => setEmail(e.target.value)} placeholder="you@example.com" />
          </div>
          <div>
            <label className="text-xs text-gray-400 mb-1 block">College (optional)</label>
            <Input value={college} onChange={(e) => setCollege(e.target.value)} placeholder="Your institution" />
          </div>
          <div>
            <label className="text-xs text-gray-400 mb-1 block">Password</label>
            <Input type="password" required minLength={6} value={password} onChange={(e) => setPassword(e.target.value)} placeholder="At least 6 characters" />
          </div>
          <PrimaryButton type="submit" disabled={loading} className="w-full">
            {loading ? 'Creating account…' : 'Create account'}
          </PrimaryButton>
        </form>

        <p className="text-center text-sm text-gray-400 mt-6">
          Already have an account? <Link to="/login" className="text-brand-400 hover:text-brand-300">Log in</Link>
        </p>
      </div>
    </div>
  );
}
