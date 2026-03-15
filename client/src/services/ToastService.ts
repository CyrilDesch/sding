import { Context, Effect, Layer, SubscriptionRef } from 'effect'
import type { ToastMessage, ToastVariant } from '../protocol/types'

// ─── Service interface ────────────────────────────────────────────────────────

export interface ToastServiceShape {
  readonly toast: SubscriptionRef.SubscriptionRef<ToastMessage | null>
  readonly show: (message: string, variant?: ToastVariant) => Effect.Effect<void>
  readonly dismiss: Effect.Effect<void>
}

export class ToastService extends Context.Tag('ToastService')<ToastService, ToastServiceShape>() {}

// ─── Live implementation ─────────────────────────────────────────────────────

let _idCounter = 0

export const ToastServiceLive = Layer.effect(
  ToastService,
  Effect.gen(function* () {
    const toastRef = yield* SubscriptionRef.make<ToastMessage | null>(null)

    return {
      toast: toastRef,

      show: (message: string, variant: ToastVariant = 'info') =>
        Effect.gen(function* () {
          const id = ++_idCounter
          yield* SubscriptionRef.set(toastRef, { id, message, variant })
          // Auto-dismiss after 4 seconds.
          yield* Effect.fork(
            Effect.sleep('4 seconds').pipe(
              Effect.flatMap(() =>
                SubscriptionRef.update(toastRef, (current) => (current?.id === id ? null : current))
              )
            )
          )
        }),

      dismiss: SubscriptionRef.set(toastRef, null),
    }
  })
)
