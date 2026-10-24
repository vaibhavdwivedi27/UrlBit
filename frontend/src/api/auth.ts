import { fetchApi } from './client';
import type { AuthResponse, UserResponse } from '@/types/api';

export interface SignupPayload {
  email: string;
  password: string;
}

export interface LoginPayload {
  email: string;
  password: string;
}

export const authApi = {
  signup: (payload: SignupPayload): Promise<UserResponse> => {
    return fetchApi<UserResponse>('/api/v1/auth/signup', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  },

  login: (payload: LoginPayload): Promise<AuthResponse> => {
    return fetchApi<AuthResponse>('/api/v1/auth/login', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  },

  logout: (): Promise<void> => {
    return fetchApi<void>('/api/v1/auth/logout', {
      method: 'POST',
    });
  },

  getMe: (): Promise<UserResponse> => {
    return fetchApi<UserResponse>('/api/v1/me', {
      method: 'GET',
    });
  },
};
