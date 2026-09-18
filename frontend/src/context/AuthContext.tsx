import { createContext, useContext, useMemo, useState, type ReactNode } from 'react';
import { apiClient } from '../api/client';
import type { AuthResponse, Role } from '../api/types';

interface AuthUser {
  userId: number;
  email: string;
  fullName: string;
  role: Role;
}

interface AuthContextValue {
  user: AuthUser | null;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, fullName: string, role: Role) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

function storeAuth(res: AuthResponse) {
  localStorage.setItem('jobmatch_token', res.token);
  localStorage.setItem(
    'jobmatch_user',
    JSON.stringify({ userId: res.userId, email: res.email, fullName: res.fullName, role: res.role }),
  );
}

function loadStoredUser(): AuthUser | null {
  const raw = localStorage.getItem('jobmatch_user');
  return raw ? (JSON.parse(raw) as AuthUser) : null;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(loadStoredUser());

  const login = async (email: string, password: string) => {
    const { data } = await apiClient.post<AuthResponse>('/api/auth/login', { email, password });
    storeAuth(data);
    setUser({ userId: data.userId, email: data.email, fullName: data.fullName, role: data.role });
  };

  const register = async (email: string, password: string, fullName: string, role: Role) => {
    const { data } = await apiClient.post<AuthResponse>('/api/auth/register', {
      email,
      password,
      fullName,
      role,
    });
    storeAuth(data);
    setUser({ userId: data.userId, email: data.email, fullName: data.fullName, role: data.role });
  };

  const logout = () => {
    localStorage.removeItem('jobmatch_token');
    localStorage.removeItem('jobmatch_user');
    setUser(null);
  };

  const value = useMemo(() => ({ user, login, register, logout }), [user]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return ctx;
}
