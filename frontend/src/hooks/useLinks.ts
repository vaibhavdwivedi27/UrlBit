import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { linksApi } from '@/api/links';
import type { CreateShortUrlRequest, UpdateShortUrlRequest } from '@/types/api';

export function useLinksList(page = 0, size = 20) {
  return useQuery({
    queryKey: ['links', page, size],
    queryFn: () => linksApi.list(page, size),
  });
}

export function useLinkDetail(id: number) {
  return useQuery({
    queryKey: ['link', id],
    queryFn: () => linksApi.getById(id),
    enabled: id > 0,
  });
}

export function useCreateLink() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: CreateShortUrlRequest) => linksApi.create(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['links'] });
    },
  });
}

export function useUpdateLink() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, request }: { id: number; request: UpdateShortUrlRequest }) =>
      linksApi.update(id, request),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['links'] });
      queryClient.invalidateQueries({ queryKey: ['link', variables.id] });
    },
  });
}

export function useDeleteLink() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => linksApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['links'] });
    },
  });
}
