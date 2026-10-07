import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api, errorMessage } from '../api/client';
import { useAuthStore } from '../store/auth';
import { PrimaryButton, Input } from '../components/ui';
import type { UserDto } from '../types';

export default function Login() {
  const navigate = useNavigate();
  const setAuth = useAuthStore((s) => s.setAuth);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = await api.post<{ token: string; user: UserDto }>('/auth/login', { email, password });
      setAuth(res.data.token, res.data.user);
      const role = res.data.user.role;
      navigate(role === 'ADMIN' ? '/admin' : role === 'RECRUITER' ? '/recruiter' : '/dashboard');
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center px-4">
      <div className="glass-card p-8 w-full max-w-md">
        <div className="text-center mb-6">
          <span className="text-3xl">🧠</span>
          <h1 className="text-2xl font-bold mt-2">Welcome back</h1>
          <p className="text-gray-400 text-sm mt-1">Log in to continue your practice</p>
        </div>

        {error && <div className="bg-red-500/10 border border-red-500/30 text-red-300 text-sm rounded-lg px-3 py-2 mb-4">{error}</div>}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="text-xs text-gray-400 mb-1 block">Email</label>
            <Input type="email" required value={email} onChange={(e) => setEmail(e.target.value)} placeholder="you@example.com" />
          </div>
          <div>
            <label className="text-xs text-gray-400 mb-1 block">Password</label>
            <Input type="password" required value={password} onChange={(e) => setPassword(e.target.value)} placeholder="••••••••" />
          </div>
          <PrimaryButton type="submit" disabled={loading} className="w-full">
            {loading ? 'Logging in…' : 'Log in'}
          </PrimaryButton>
        </form>

        <div className="mt-4 text-xs text-gray-500 bg-white/5 rounded-lg p-3">
          Demo: <span className="text-gray-300">demo@codesim.ai</span> / demo123 · Admin:{' '}
          <span className="text-gray-300">admin@codesim.ai</span> / admin123
        </div>

        <p className="text-center text-sm text-gray-400 mt-6">
          No account? <Link to="/register" className="text-brand-400 hover:text-brand-300">Sign up</Link>
        </p>
      </div>
    </div>
  );
}
