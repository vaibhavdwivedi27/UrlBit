import type { ProblemDetail } from '@/types/api';

/**
 * Custom error class wrapping RFC 9457 Problem Details returned by the backend.
 */
export class ApiError extends Error {
  public readonly status: number;
  public readonly problem?: ProblemDetail;

  constructor(message: string, status: number, problem?: ProblemDetail) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.problem = problem;
  }

  get isQuotaExceeded(): boolean {
    return this.problem?.type?.includes('guest-quota-exceeded') || this.status === 429;
  }

  get isAliasTaken(): boolean {
    return this.problem?.type?.includes('alias-taken') || false;
  }

  get isRateLimited(): boolean {
    return this.problem?.type?.includes('rate-limited') || false;
  }

  get isUnsafeDestination(): boolean {
    return this.problem?.type?.includes('unsafe-destination') || false;
  }
}

/**
 * Universal fetch wrapper for Urlbit backend APIs.
 * Automatically injects credentials for HttpOnly cookie authentication and parses RFC 9457 errors.
 */
export async function fetchApi<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const baseUrl = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '');
  const url = endpoint.startsWith('http') ? endpoint : `${baseUrl}${endpoint}`;

  const headers = new Headers(options.headers || {});
  if (!headers.has('Content-Type') && !(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json');
  }

  const response = await fetch(url, {
    ...options,
    headers,
    credentials: 'include', // Essential for sending and receiving HttpOnly auth cookies
  });

  if (response.status === 204) {
    return null as T;
  }

  const text = await response.text();
  let data: any = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = text;
    }
  }

  if (!response.ok) {
    const problem: ProblemDetail | undefined = typeof data === 'object' && data !== null ? data : undefined;
    const message = problem?.detail || problem?.title || `Request failed with status ${response.status}`;
    throw new ApiError(message, response.status, problem);
  }

  return data as T;
}
