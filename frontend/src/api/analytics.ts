import { fetchApi } from './client';
import type { UrlAnalyticsResponse } from '@/types/api';

export const analyticsApi = {
  getByLinkId: (id: number): Promise<UrlAnalyticsResponse> => {
    return fetchApi<UrlAnalyticsResponse>(`/api/v1/analytics/urls/${id}`, {
      method: 'GET',
    });
  },
};
