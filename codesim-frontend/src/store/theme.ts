import { create } from 'zustand';
import { persist } from 'zustand/middleware';

export type Theme = 'light' | 'dark';

interface ThemeState {
  theme: Theme;
  setTheme: (theme: Theme) => void;
  toggleTheme: () => void;
}

export const useThemeStore = create<ThemeState>()(
  persist(
    (set, get) => ({
      theme: 'dark',
      setTheme: (theme) => {
        document.documentElement.setAttribute('data-theme', theme);
        set({ theme });
      },
      toggleTheme: () => {
        const next = get().theme === 'dark' ? 'light' : 'dark';
        document.documentElement.setAttribute('data-theme', next);
        set({ theme: next });
      },
    }),
    {
      name: 'codesim-theme',
      onRehydrateStorage: () => (state) => {
        if (state?.theme) {
          document.documentElement.setAttribute('data-theme', state.theme);
        }
      },
    },
  ),
);

export function initTheme() {
  const stored = localStorage.getItem('codesim-theme');
  let theme: Theme = 'dark';
  if (stored) {
    try {
      const parsed = JSON.parse(stored);
      if (parsed?.state?.theme === 'light' || parsed?.state?.theme === 'dark') {
        theme = parsed.state.theme;
      }
    } catch {
      /* use default */
    }
  }
  document.documentElement.setAttribute('data-theme', theme);
}
