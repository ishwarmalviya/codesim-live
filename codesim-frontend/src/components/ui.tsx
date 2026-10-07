import type { ReactNode } from 'react';

export function Card({ children, className = '' }: { children: ReactNode; className?: string }) {
  return <div className={`glass-card p-5 ${className}`}>{children}</div>;
}

export function StatCard({ label, value, icon }: { label: string; value: ReactNode; icon: string }) {
  return (
    <Card className="flex items-center gap-4">
      <div className="w-11 h-11 rounded-xl grad-brand flex items-center justify-center text-xl shrink-0">{icon}</div>
      <div className="min-w-0">
        <div className="text-xs text-gray-400 truncate">{label}</div>
        <div className="text-xl font-bold truncate">{value}</div>
      </div>
    </Card>
  );
}

export function Spinner({ className = '' }: { className?: string }) {
  return (
    <div className={`inline-block w-5 h-5 border-2 border-white/20 border-t-brand-400 rounded-full animate-spin ${className}`} />
  );
}

export function Badge({ children, tone = 'brand' }: { children: ReactNode; tone?: 'brand' | 'green' | 'red' | 'yellow' | 'gray' }) {
  const tones: Record<string, string> = {
    brand: 'bg-brand-500/15 text-brand-300 border-brand-500/30',
    green: 'bg-emerald-500/15 text-emerald-300 border-emerald-500/30',
    red: 'bg-red-500/15 text-red-300 border-red-500/30',
    yellow: 'bg-amber-500/15 text-amber-300 border-amber-500/30',
    gray: 'bg-gray-500/15 text-gray-300 border-gray-500/30',
  };
  return <span className={`text-xs px-2 py-0.5 rounded-full border font-medium ${tones[tone]}`}>{children}</span>;
}

export function PrimaryButton({
  children,
  onClick,
  disabled,
  type = 'button',
  className = '',
}: {
  children: ReactNode;
  onClick?: () => void;
  disabled?: boolean;
  type?: 'button' | 'submit';
  className?: string;
}) {
  return (
    <button
      type={type}
      onClick={onClick}
      disabled={disabled}
      className={`btn-primary text-white font-semibold px-5 py-2.5 rounded-lg disabled:opacity-50 disabled:cursor-not-allowed ${className}`}
    >
      {children}
    </button>
  );
}

export function Input(props: React.InputHTMLAttributes<HTMLInputElement>) {
  return (
    <input
      {...props}
      className={`w-full bg-white/5 border border-white/10 rounded-lg px-3 py-2.5 text-sm focus:outline-none focus:border-brand-500 transition ${props.className ?? ''}`}
    />
  );
}

export function Select(props: React.SelectHTMLAttributes<HTMLSelectElement>) {
  return (
    <select
      {...props}
      className={`codesim-select w-full bg-[var(--bg-secondary)] text-[var(--text-primary)] border border-white/10 rounded-lg px-3 py-2.5 text-sm focus:outline-none focus:border-brand-500 transition ${props.className ?? ''}`}
    />
  );
}
