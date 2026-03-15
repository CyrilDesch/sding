import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { MutationCache, QueryCache, QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { createHashHistory, createRouter, RouterProvider } from '@tanstack/react-router'
import { Toaster } from 'sonner'
import { UnauthorizedError } from './api/errors'
import { authQueryOptions } from './api/auth'
import { routeTree } from './routeTree.gen'
import './index.css'

// ─── QueryClient ──────────────────────────────────────────────────────────────
// Closures reference queryClient/router by binding — both are fully assigned
// before any query or mutation can fire, so no TDZ issues.

const handle401 = () => {
  queryClient.setQueryData(authQueryOptions.queryKey, false)
  void router.navigate({ to: '/login' })
}

const queryClient = new QueryClient({
  queryCache: new QueryCache({
    onError: (error) => {
      if (error instanceof UnauthorizedError) handle401()
    },
  }),
  mutationCache: new MutationCache({
    onError: (error) => {
      if (error instanceof UnauthorizedError) handle401()
    },
  }),
  defaultOptions: {
    queries: { retry: false, staleTime: 30_000 },
  },
})

// ─── Router ───────────────────────────────────────────────────────────────────

const router = createRouter({
  routeTree,
  history: createHashHistory(),
  context: { queryClient },
})

declare module '@tanstack/react-router' {
  interface Register {
    router: typeof router
  }
}

// ─── Mount ────────────────────────────────────────────────────────────────────

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
      <Toaster position="bottom-center" richColors />
    </QueryClientProvider>
  </StrictMode>
)
