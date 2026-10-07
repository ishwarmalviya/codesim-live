import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { UserDto } from '../types';

interface AuthState {
  token: string | null;
  user: UserDto | null;
  setAuth: (token: string, user: UserDto) => void;
  updateUser: (user: UserDto) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      user: null,
      setAuth: (token, user) => set({ token, user }),
      updateUser: (user) => set({ user }),
      logout: () => set({ token: null, user: null }),
    }),
    { name: 'codesim-auth' }
  )
);
