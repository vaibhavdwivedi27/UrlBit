import { useQuery } from '@tanstack/react-query';
import { analyticsApi } from '@/api/analytics';

export function useLinkAnalytics(linkId: number) {
  return useQuery({
    queryKey: ['analytics', linkId],
    queryFn: () => analyticsApi.getByLinkId(linkId),
    enabled: linkId > 0,
    refetchInterval: 10000, // Auto-refresh analytics every 10 seconds for live demo
  });
}
