/**
 * Strict TypeScript types matching Spring Boot backend DTO records and RFC 9457 Problem Details.
 */

export type ShortUrlStatus = 'ACTIVE' | 'DISABLED' | 'DELETED';
export type DisabledReason = 'OWNER' | 'ABUSE_TAKEDOWN';
export type UserRole = 'USER' | 'TENANT' | 'ADMIN';
export type UserStatus = 'ACTIVE' | 'DISABLED';

export interface ShortUrlResponse {
  id: number;
  code: string;
  shortUrl: string;
  longUrl: string;
  status: ShortUrlStatus;
  expiresAt: string | null;
  createdAt: string;
}

export interface CreateShortUrlRequest {
  longUrl: string;
  customAlias?: string | null;
  expiresAt?: string | null;
}

export interface UpdateShortUrlRequest {
  status?: ShortUrlStatus | null;
  expiresAt?: string | null;
}

export interface UserResponse {
  id: string;
  email: string;
  status: UserStatus;
  role: UserRole;
  createdAt: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  expiresInSeconds: number;
  tokenType: string;
}

export interface DailyClick {
  day: string;
  clicks: number;
}

export interface ReferrerStat {
  referrerHost: string;
  clicks: number;
}

export interface UrlAnalyticsResponse {
  shortUrlId: number;
  totalClicks: number;
  dailyClicks: DailyClick[];
  topReferrers: ReferrerStat[];
}

export interface ProblemDetail {
  type: string;
  title: string;
  status: number;
  detail: string;
  instance?: string;
  errors?: string[];
  [key: string]: unknown;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}
