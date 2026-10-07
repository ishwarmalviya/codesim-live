import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client';
import { useThemeStore } from '../store/theme';
import { PrimaryButton } from '../components/ui';

interface Stats {
  totalUsers: number;
  totalQuestions: number;
  totalInterviews: number;
  topics: number;
}

const features = [
  { icon: '🎯', title: 'Adaptive Engine', desc: 'Elo-based difficulty that adjusts to your skill after every question.' },
  { icon: '🤖', title: 'AI Code Review', desc: 'GPT-4o powered feedback on correctness, complexity, and style.' },
  { icon: '📊', title: 'Deep Analytics', desc: 'Track proficiency by topic, Elo trends, and interview history.' },
  { icon: '⚡', title: 'Live Execution', desc: 'Sandboxed code execution with instant multi-test-case results.' },
  { icon: '💡', title: 'Progressive Hints', desc: 'Socratic hints that guide without giving away the solution.' },
  { icon: '🛠️', title: 'Admin Panel', desc: 'Full question bank management and platform analytics.' },
];

function ContactForm({ type, title }: { type: string; title: string }) {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [message, setMessage] = useState('');
  const [sent, setSent] = useState(false);
  const [error, setError] = useState('');

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    try {
      await api.post('/public/contact', { name, email, message, type });
      setSent(true);
      setName('');
      setEmail('');
      setMessage('');
    } catch {
      setError('Failed to send message. Please try again.');
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <h3 className="font-semibold text-lg">{title}</h3>
      {sent && <p className="text-sm text-emerald-400">Message sent successfully!</p>}
      {error && <p className="text-sm text-red-400">{error}</p>}
      <input
        required
        value={name}
        onChange={(e) => setName(e.target.value)}
        placeholder="Your name"
        className="w-full bg-white/5 border border-white/10 rounded-lg px-3 py-2.5 text-sm focus:outline-none focus:border-brand-500"
      />
      <input
        required
        type="email"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        placeholder="Email address"
        className="w-full bg-white/5 border border-white/10 rounded-lg px-3 py-2.5 text-sm focus:outline-none focus:border-brand-500"
      />
      <textarea
        required
        value={message}
        onChange={(e) => setMessage(e.target.value)}
        rows={4}
        placeholder="Your message"
        className="w-full bg-white/5 border border-white/10 rounded-lg px-3 py-2.5 text-sm focus:outline-none focus:border-brand-500"
      />
      <PrimaryButton type="submit" className="w-full">Send Message</PrimaryButton>
    </form>
  );
}

export default function Landing() {
  const [stats, setStats] = useState<Stats | null>(null);
  const [daily, setDaily] = useState<{ available?: boolean; question?: { id: number; title: string; difficulty: string } } | null>(null);
  const { theme, toggleTheme } = useThemeStore();

  useEffect(() => {
    api.get<Stats>('/public/stats').then((res) => setStats(res.data)).catch(() => {});
    api.get('/public/daily-question').then((res) => setDaily(res.data)).catch(() => {});
  }, []);

  return (
    <div className="min-h-screen">
      <nav className="max-w-6xl mx-auto flex items-center justify-between px-6 py-5">
        <div className="flex items-center gap-2">
          <span className="text-2xl">🧠</span>
          <span className="font-bold text-lg grad-text">CodeSim AI</span>
        </div>
        <div className="flex items-center gap-3">
          <button onClick={toggleTheme} className="text-sm text-gray-400 hover:text-white px-2 py-2" title="Toggle theme">
            {theme === 'dark' ? '☀️' : '🌙'}
          </button>
          <a href="#about" className="text-sm text-gray-300 hover:text-white px-3 py-2 hidden sm:inline">About</a>
          <a href="#contact" className="text-sm text-gray-300 hover:text-white px-3 py-2 hidden sm:inline">Contact</a>
          <Link to="/login" className="text-sm text-gray-300 hover:text-white px-3 py-2">Log in</Link>
          <Link to="/register" className="btn-primary text-white text-sm font-semibold px-4 py-2 rounded-lg">Get Started</Link>
        </div>
      </nav>

      <section className="max-w-4xl mx-auto text-center px-6 pt-16 pb-20">
        <h1 className="text-4xl md:text-6xl font-extrabold leading-tight">
          Master coding interviews with <span className="grad-text">Adaptive AI</span>
        </h1>
        <p className="text-gray-400 mt-6 text-lg max-w-2xl mx-auto">
          Practice with an Elo-rated adaptive engine, get instant AI-powered code reviews, and track your
          growth toward placement-ready proficiency.
        </p>
        <div className="mt-8 flex items-center justify-center gap-4 flex-wrap">
          <Link to="/register" className="btn-primary text-white font-semibold px-6 py-3 rounded-lg">Start Practicing Free</Link>
          <Link to="/login" className="glass-card px-6 py-3 rounded-lg font-semibold hover:bg-white/5 transition">Demo Login</Link>
        </div>

        {daily?.available && daily.question && (
          <div className="glass-card p-5 mt-12 max-w-lg mx-auto text-left">
            <div className="text-xs uppercase text-brand-400 font-semibold mb-2">Daily Challenge</div>
            <div className="font-bold text-lg">{daily.question.title}</div>
            <div className="text-sm text-gray-400 mt-1 capitalize">{daily.question.difficulty}</div>
            <Link to="/register" className="text-sm text-brand-400 mt-3 inline-block hover:underline">Sign up to solve →</Link>
          </div>
        )}

        {stats && (
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mt-16">
            {[
              { label: 'Learners', value: stats.totalUsers },
              { label: 'Questions', value: stats.totalQuestions },
              { label: 'Interviews Run', value: stats.totalInterviews },
              { label: 'Topics Covered', value: stats.topics },
            ].map((s) => (
              <div key={s.label} className="glass-card p-4">
                <div className="text-2xl font-bold grad-text">{s.value}</div>
                <div className="text-xs text-gray-400 mt-1">{s.label}</div>
              </div>
            ))}
          </div>
        )}
      </section>

      <section className="max-w-6xl mx-auto px-6 pb-16">
        <div className="grid md:grid-cols-3 gap-5">
          {features.map((f) => (
            <div key={f.title} className="glass-card p-6">
              <div className="text-3xl mb-3">{f.icon}</div>
              <h3 className="font-semibold text-lg mb-2">{f.title}</h3>
              <p className="text-sm text-gray-400">{f.desc}</p>
            </div>
          ))}
        </div>
      </section>

      <section id="about" className="max-w-4xl mx-auto px-6 py-16 border-t border-white/10">
        <h2 className="text-3xl font-bold mb-4">About Us</h2>
        <p className="text-gray-300 leading-relaxed mb-4">
          CodeSim AI is an AI-Driven Adaptive Coding Interview Simulator designed to help students and professionals
          prepare for technical interviews at top companies. Our platform combines Elo-rated adaptive question selection,
          real-time code execution, AI-powered code review, and company-specific readiness tracking.
        </p>
        <p className="text-gray-400 leading-relaxed">
          Whether you are targeting FAANG, product companies, or service-based roles, CodeSim personalizes your practice
          path based on your strengths, weaknesses, and target company requirements.
        </p>
      </section>

      <section id="contact" className="max-w-6xl mx-auto px-6 py-16 border-t border-white/10">
        <div className="grid md:grid-cols-2 gap-8">
          <div className="glass-card p-6">
            <ContactForm type="contact" title="Contact Us" />
          </div>
          <div className="glass-card p-6">
            <ContactForm type="admin_message" title="Message to Admin" />
          </div>
        </div>
      </section>

      <footer className="text-center text-gray-500 text-sm pb-10 border-t border-white/10 pt-8">
        Built for the AI-Driven Adaptive Coding Interview Simulator &amp; Dynamic Review Engine.
      </footer>
    </div>
  );
}
