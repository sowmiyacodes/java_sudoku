import { createContext } from 'react';
import type { RegisterData, UserProfile } from '../services/authApi';

export interface AuthContextValue {
  user: UserProfile | null;
  isLoading: boolean;
  register: (data: RegisterData) => Promise<void>;
  login: (identifier: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  updateProfile: (data: Pick<UserProfile, 'displayName' | 'email'>) => Promise<void>;
}

export const AuthContext = createContext<AuthContextValue | null>(null);