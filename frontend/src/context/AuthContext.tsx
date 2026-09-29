import { useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import { AuthApi } from '../services/authApi';
import type { RegisterData, UserProfile } from '../services/authApi';
import { AuthContext } from './authStateContext';

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserProfile | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    AuthApi.currentUser()
      .then(setUser)
      .catch(() => setUser(null))
      .finally(() => setIsLoading(false));
  }, []);

  const register = async (data: RegisterData) => setUser(await AuthApi.register(data));
  const login = async (identifier: string, password: string) => setUser(await AuthApi.login(identifier, password));
  const logout = async () => {
    await AuthApi.logout();
    setUser(null);
  };
  const updateProfile = async (data: Pick<UserProfile, 'displayName' | 'email'>) => setUser(await AuthApi.updateProfile(data));

  return (
    <AuthContext.Provider value={{ user, isLoading, register, login, logout, updateProfile }}>
      {children}
    </AuthContext.Provider>
  );
}