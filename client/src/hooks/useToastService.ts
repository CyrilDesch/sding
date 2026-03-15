import { Effect } from 'effect'
import { useCallback, useMemo } from 'react'
import type { ToastVariant } from '../protocol/types'
import { ToastService } from '../services/ToastService'
import { useRuntime } from '../runtime/useRuntime'
import { useSubscriptionRef } from './useEffectTsValue'

export function useToastService() {
  const runtime = useRuntime()

  const toastRef = useMemo(
    () => runtime.runSync(ToastService.pipe(Effect.map((svc) => svc.toast))),
    [runtime]
  )
  const toast = useSubscriptionRef(toastRef, null)

  const show = useCallback(
    (message: string, variant?: ToastVariant) =>
      runtime.runFork(ToastService.pipe(Effect.flatMap((svc) => svc.show(message, variant)))),
    [runtime]
  )

  const dismiss = useCallback(
    () => runtime.runFork(ToastService.pipe(Effect.flatMap((svc) => svc.dismiss))),
    [runtime]
  )

  return { toast, show, dismiss }
}
