import { fetchApi } from './client';

export interface HealthResponse {
  status: 'UP' | 'DOWN' | 'UNKNOWN';
  components?: Record<string, { status: string; details?: Record<string, any> }>;
}

export const healthApi = {
  checkReadiness: (): Promise<HealthResponse> => {
    return fetchApi<HealthResponse>('/actuator/health/readiness', {
      method: 'GET',
    });
  },
};
