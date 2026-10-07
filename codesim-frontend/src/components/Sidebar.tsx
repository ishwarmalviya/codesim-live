import { NavLink } from 'react-router-dom';
import { useEffect } from 'react';
import { useAuthStore } from '../store/auth';
import { useThemeStore } from '../store/theme';
import { api } from '../api/client';

const userLinks = [
  { to: '/dashboard', label: 'Dashboard', icon: '📊' },
  { to: '/practice', label: 'Practice', icon: '📚' },
  { to: '/interview/start', label: 'Interview Mode', icon: '🚀' },
  { to: '/submissions', label: 'Submissions', icon: '📋' },
  { to: '/companies', label: 'Target Companies', icon: '🎯' },
  { to: '/roadmap', label: 'Skill Roadmap', icon: '🗺️' },
  { to: '/resume', label: 'Resume', icon: '📄' },
  { to: '/profile', label: 'Profile', icon: '👤' },
  { to: '/leaderboard', label: 'Leaderboard', icon: '🏆' },
];

const adminLinks = [
  { to: '/admin', label: 'Overview', icon: '📈' },
  { to: '/admin/users', label: 'Users', icon: '👥' },
  { to: '/admin/questions', label: 'Questions', icon: '📝' },
  { to: '/admin/analytics', label: 'Analytics', icon: '📊' },
  { to: '/admin/messages', label: 'Messages', icon: '✉️' },
];

const recruiterLinks = [
  { to: '/recruiter', label: 'Candidate Search', icon: '🔍' },
  { to: '/recruiter/shortlist', label: 'Shortlist', icon: '⭐' },
];

export function Sidebar() {
  const user = useAuthStore((s) => s.user);
  const logout = useAuthStore((s) => s.logout);
  const { theme, toggleTheme, setTheme } = useThemeStore();
  const links = user?.role === 'ADMIN' ? adminLinks : user?.role === 'RECRUITER' ? recruiterLinks : userLinks;

  useEffect(() => {
    if (user?.themePreference) {
      setTheme(user.themePreference as 'light' | 'dark');
    }
  }, [user?.themePreference, setTheme]);

  async function handleThemeToggle() {
    const next = theme === 'dark' ? 'light' : 'dark';
    toggleTheme();
    if (user) {
      try {
        await api.put('/profile', { themePreference: next });
      } catch {
        /* local preference still applies */
      }
    }
  }

  return (
    <aside className="w-64 shrink-0 glass-card m-4 mr-0 p-4 flex flex-col h-[calc(100vh-2rem)] sticky top-4">
      <div className="flex items-center gap-2 mb-8 px-2">
        <span className="text-2xl">🧠</span>
        <span className="font-bold text-lg grad-text">CodeSim AI</span>
      </div>

      <nav className="flex flex-col gap-1 flex-1">
        {links.map((l) => (
          <NavLink
            key={l.to}
            to={l.to}
            end={l.to === '/admin'}
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition ${
                isActive ? 'bg-brand-600/20 text-brand-300' : 'text-gray-400 hover:bg-white/5 hover:text-gray-200'
              }`
            }
          >
            <span>{l.icon}</span>
            {l.label}
          </NavLink>
        ))}
      </nav>

      {user && user.role === 'USER' && (
        <div className="px-3 py-2 mb-2">
          <div className="flex justify-between text-xs text-gray-400 mb-1">
            <span>Elo Rating</span>
            <span className="text-brand-300 font-semibold">{Math.round(user.eloRating)}</span>
          </div>
          <div className="h-1.5 bg-white/10 rounded-full overflow-hidden">
            <div
              className="h-full grad-brand rounded-full"
              style={{ width: `${Math.min(100, (user.eloRating / 2000) * 100)}%` }}
            />
          </div>
          <div className="text-xs text-gray-500 mt-1">{user.skillLevel}</div>
        </div>
      )}

      <div className="px-3 py-2 border-t border-white/10 pt-3 flex items-center justify-between gap-2">
        <button
          onClick={handleThemeToggle}
          title="Toggle theme"
          className="text-gray-400 hover:text-white text-sm shrink-0"
        >
          {theme === 'dark' ? '☀️' : '🌙'}
        </button>
        <div className="text-sm truncate flex-1 min-w-0">
          <div className="font-medium truncate">{user?.name}</div>
          <div className="text-xs text-gray-500 truncate">{user?.email}</div>
        </div>
        <button
          onClick={() => {
            logout();
            // Full navigation avoids ProtectedRoute racing to /login after token clear
            window.location.assign('/');
          }}
          title="Logout"
          aria-label="Logout"
          className="text-gray-400 hover:text-red-400 transition"
        >
          ⏻
        </button>
      </div>
    </aside>
  );
}
