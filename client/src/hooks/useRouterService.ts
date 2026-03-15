import { Effect } from 'effect'
import { useCallback, useMemo } from 'react'
import type { Page } from '../protocol/types'
import { RouterService } from '../services/RouterService'
import { useRuntime } from '../runtime/useRuntime'
import { useSubscriptionRef } from './useEffectTsValue'

export function useRouterService() {
  const runtime = useRuntime()

  const currentPageRef = useMemo(
    () => runtime.runSync(RouterService.pipe(Effect.map((svc) => svc.currentPage))),
    [runtime]
  )
  const currentPage = useSubscriptionRef(currentPageRef, { type: 'landing' } as Page)

  const navigate = useCallback(
    (page: Page) =>
      runtime.runFork(RouterService.pipe(Effect.flatMap((svc) => svc.navigate(page)))),
    [runtime]
  )

  return { currentPage, navigate }
}
