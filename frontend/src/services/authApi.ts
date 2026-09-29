import { apiFetch, resetCsrfToken } from './http';

export interface UserProfile {
  id: number;
  username: string;
  displayName: string;
  email: string;
  createdAt: string;
}

export interface RegisterData {
  username: string;
  displayName: string;
  email: string;
  password: string;
}

async function readResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const body = await response.json().catch(() => null) as { message?: string; detail?: string } | null;
    throw new Error(body?.message ?? body?.detail ?? `Request failed (${response.status}).`);
  }
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

export const AuthApi = {
  async register(data: RegisterData): Promise<UserProfile> {
    return readResponse(await apiFetch('/api/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    }));
  },

  async login(identifier: string, password: string): Promise<UserProfile> {
    return readResponse(await apiFetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ identifier, password }),
    }));
  },

  async logout(): Promise<void> {
    try {
      await readResponse(await apiFetch('/api/auth/logout', { method: 'POST' }));
    } finally {
      resetCsrfToken();
    }
  },

  async currentUser(): Promise<UserProfile> {
    return readResponse(await apiFetch('/api/auth/me'));
  },

  async updateProfile(data: Pick<UserProfile, 'displayName' | 'email'>): Promise<UserProfile> {
    return readResponse(await apiFetch('/api/auth/me', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    }));
  },
};