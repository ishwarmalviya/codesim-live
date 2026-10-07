import { useState, type ReactNode } from 'react';
import { Badge } from './ui';

type Props = {
  title: string;
  difficulty: string;
  topic: string;
  description: string;
  examples?: string | null;
  constraints?: string | null;
  defaultOpen?: boolean;
  children?: ReactNode;
};

function difficultyTone(difficulty: string) {
  const d = difficulty.toLowerCase();
  if (d === 'easy') return 'green' as const;
  if (d === 'medium') return 'yellow' as const;
  return 'red' as const;
}

export function ProblemAccordion({
  title,
  difficulty,
  topic,
  description,
  examples,
  constraints,
  defaultOpen = true,
  children,
}: Props) {
  const [open, setOpen] = useState(defaultOpen);

  function toggle() {
    setOpen((v) => !v);
  }

  return (
    <section className="problem-accordion mx-1">
      <button
        type="button"
        onClick={toggle}
        aria-expanded={open}
        className="problem-accordion-header"
      >
        <div className="flex items-center gap-2 flex-wrap min-w-0">
          <h2 className="text-base font-bold text-[var(--text-primary)] truncate">{title}</h2>
          <Badge tone={difficultyTone(difficulty)}>{difficulty}</Badge>
          <Badge tone="gray">{topic}</Badge>
        </div>
        <span className="problem-accordion-toggle shrink-0">
          {open ? 'Hide ▲' : 'Show ▼'}
        </span>
      </button>

      <div className={`accordion-collapse ${open ? 'is-open' : ''}`}>
        <div className="accordion-collapse-inner">
          <div className="accordion-collapse-panel">
            <p className="text-sm text-gray-300 whitespace-pre-wrap leading-relaxed">{description}</p>
            {examples && (
              <div className="mt-3">
                <h4 className="text-xs uppercase tracking-wide text-gray-500 font-semibold mb-1.5">Example</h4>
                <pre className="problem-example-block">{examples}</pre>
              </div>
            )}
            {constraints && (
              <div className="mt-3">
                <h4 className="text-xs uppercase tracking-wide text-gray-500 font-semibold mb-1.5">Constraints</h4>
                <pre className="problem-example-block">{constraints}</pre>
              </div>
            )}
            {children}
          </div>
        </div>
      </div>
    </section>
  );
}
