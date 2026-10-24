import { fetchApi } from './client';
import type {
  CreateShortUrlRequest,
  PageResponse,
  ShortUrlResponse,
  UpdateShortUrlRequest,
} from '@/types/api';

export const linksApi = {
  create: (payload: CreateShortUrlRequest): Promise<ShortUrlResponse> => {
    return fetchApi<ShortUrlResponse>('/api/v1/urls', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  },

  getById: (id: number): Promise<ShortUrlResponse> => {
    return fetchApi<ShortUrlResponse>(`/api/v1/urls/${id}`, {
      method: 'GET',
    });
  },

  list: (page = 0, size = 20): Promise<PageResponse<ShortUrlResponse>> => {
    return fetchApi<PageResponse<ShortUrlResponse>>(`/api/v1/urls?page=${page}&size=${size}&sort=createdAt,desc`, {
      method: 'GET',
    });
  },

  update: (id: number, payload: UpdateShortUrlRequest): Promise<ShortUrlResponse> => {
    return fetchApi<ShortUrlResponse>(`/api/v1/urls/${id}`, {
      method: 'PATCH',
      body: JSON.stringify(payload),
    });
  },

  delete: (id: number): Promise<void> => {
    return fetchApi<void>(`/api/v1/urls/${id}`, {
      method: 'DELETE',
    });
  },
};
