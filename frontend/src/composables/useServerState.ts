// frontend/src/composables/useServerState.ts
// TanStack Query wrapper — unified server state management
// Rules: No Pinia direct storage of API data. All server state goes through this composable.

import {
  useQuery,
  useMutation,
  useInfiniteQuery,
  useQueryClient,
  type UseQueryOptions,
  type UseMutationOptions,
  type QueryClient,
} from '@tanstack/vue-query'
import { ref, computed } from 'vue'
import type { Ref } from 'vue'

// Default retry config
const DEFAULT_RETRY = 2
const DEFAULT_STALE_TIME = 5 * 60 * 1000 // 5 minutes
const DEFAULT_GC_TIME = 30 * 60 * 1000 // 30 minutes

// Base API fetcher
async function apiFetch<T>(url: string, options?: RequestInit): Promise<T> {
  // Get CSRF token from meta tag (if present)
  const csrfToken = typeof document !== 'undefined'
    ? document.querySelector<HTMLMetaElement>('meta[name="csrf-token"]')?.content
    : undefined

  const response = await fetch(url, {
    headers: {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
      ...(csrfToken ? { 'X-CSRF-Token': csrfToken } : {}),
    },
    // Use 'include' for proxied API calls, 'same-origin' for direct calls
    credentials: import.meta.env.PROD ? 'same-origin' : 'include',
    ...options,
  })

  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: response.statusText }))
    throw new ApiError(error.message || `HTTP ${response.status}`, response.status)
  }

  return response.json()
}

export class ApiError extends Error {
  constructor(message: string, public status: number) {
    super(message)
    this.name = 'ApiError'
  }
}

// ─── useGet: Server data fetching ───

interface UseGetOptions<T> extends Partial<UseQueryOptions<T>> {
  params?: Record<string, string | number | boolean>
  enabled?: Ref<boolean> | boolean
  staleTime?: number
}

export function useGet<T>(key: string, url: string, options?: UseGetOptions<T>) {
  const queryKey = [key, options?.params]

  const enabled = computed(() => {
    if (options?.enabled === undefined) return true
    return typeof options.enabled === 'boolean' ? options.enabled : options.enabled.value
  })

  const fetchUrl = computed(() => {
    if (!options?.params) return url
    const searchParams = new URLSearchParams()
    for (const [k, v] of Object.entries(options.params)) {
      searchParams.append(k, String(v))
    }
    return `${url}?${searchParams.toString()}`
  })

  const result = useQuery<T, ApiError>({
    queryKey,
    queryFn: () => apiFetch<T>(fetchUrl.value),
    retry: DEFAULT_RETRY,
    staleTime: options?.staleTime ?? DEFAULT_STALE_TIME,
    gcTime: DEFAULT_GC_TIME,
    enabled,
    refetchOnWindowFocus: true,
    refetchOnReconnect: true,
    ...options,
  })

  return {
    ...result,
    isPending: result.isLoading,
  }
}

// ─── usePost/usePut/useDelete: Mutations ───

interface UseMutateOptions<TData, TVariables>
  extends Partial<UseMutationOptions<TData, ApiError, TVariables>> {
  invalidateKeys?: string[]
  optimisticUpdate?: (variables: TVariables) => void
  rollbackOptimistic?: (variables: TVariables, previous: TData | undefined) => void
}

function createMutation<TData, TVariables>(
  method: string,
  url: string,
  options?: UseMutateOptions<TData, TVariables>
) {
  const queryClient = useQueryClient()

  return useMutation<TData, ApiError, TVariables>({
    mutationFn: (variables: TVariables) =>
      apiFetch<TData>(url, {
        method,
        body: typeof variables === 'string' ? variables : JSON.stringify(variables),
      }),
    onMutate: async (variables) => {
      if (options?.optimisticUpdate) {
        options.optimisticUpdate(variables)
      }
      return { previous: undefined }
    },
    onError: (error, variables, context) => {
      if (options?.rollbackOptimistic && context?.previous) {
        options.rollbackOptimistic(variables, context.previous as TData)
      }
    },
    onSuccess: (data) => {
      if (options?.invalidateKeys) {
        for (const key of options.invalidateKeys) {
          queryClient.invalidateQueries({ queryKey: [key] })
        }
      }
    },
    ...options,
  })
}

export function usePost<TData, TVariables = unknown>(url: string, options?: UseMutateOptions<TData, TVariables>) {
  return createMutation<TData, TVariables>('POST', url, options)
}

export function usePut<TData, TVariables = unknown>(url: string, options?: UseMutateOptions<TData, TVariables>) {
  return createMutation<TData, TVariables>('PUT', url, options)
}

export function useDelete<TData = void, TVariables = unknown>(url: string, options?: UseMutateOptions<TData, TVariables>) {
  return createMutation<TData, TVariables>('DELETE', url, options)
}

// ─── usePaginated: Server-side pagination ───

interface PaginatedResponse<T> {
  data: T[]
  total: number
  page: number
  pageSize: number
}

export function usePaginated<T>(key: string, baseUrl: string, pageSize = 20) {
  const currentPage = ref(1)

  const queryKey = computed(() => [key, baseUrl, currentPage.value, pageSize])
  const fetchUrl = computed(() => `${baseUrl}?page=${currentPage.value - 1}&size=${pageSize}`)

  const result = useQuery<PaginatedResponse<T>, ApiError>({
    queryKey,
    queryFn: () => apiFetch<PaginatedResponse<T>>(fetchUrl.value),
    staleTime: 0, // Always refetch on page change
    enabled: computed(() => currentPage.value > 0),
    refetchOnWindowFocus: true,
    refetchOnReconnect: true,
  })

  return {
    ...result,
    currentPage,
    nextPage: () => { currentPage.value++ },
    prevPage: () => { if (currentPage.value > 1) currentPage.value-- },
    goToPage: (page: number) => { currentPage.value = page },
  }
}

// ─── useOptimisticMutation: Optimistic updates ───

export function useOptimisticMutation<TData, TVariables, TCache>(
  url: string,
  method: string,
  cacheKey: string,
  updateFn: (previous: TCache, variables: TVariables) => TCache,
  options?: UseMutateOptions<TData, TVariables>
) {
  const queryClient = useQueryClient()

  return useMutation<TData, ApiError, TVariables>({
    mutationFn: (variables) =>
      apiFetch<TData>(url, {
        method,
        body: JSON.stringify(variables),
      }),
    onMutate: async (variables) => {
      // Cancel outgoing refetches
      await queryClient.cancelQueries({ queryKey: [cacheKey] })

      // Snapshot previous value
      const previous = queryClient.getQueryData<TCache>([cacheKey])

      // Optimistically update
      if (previous) {
        queryClient.setQueryData([cacheKey], updateFn(previous, variables))
      }

      return { previous }
    },
    onError: (error, variables, context) => {
      if (context?.previous) {
        queryClient.setQueryData([cacheKey], context.previous)
      }
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: [cacheKey] })
    },
    ...options,
  })
}
