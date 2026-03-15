import {
  useLayoutEffect,
  useRef,
  useSyncExternalStore,
  useCallback,
  type DependencyList,
} from 'react'
import { Effect, Fiber, Stream, SubscriptionRef } from 'effect'
import { useRuntime } from '../runtime/useRuntime'
import type { AppServices } from '../runtime/AppLayer'

// ------------------------------------------------------------------ //
// useEffectTsValue — subscribes to an Effect's output via             //
// useSyncExternalStore (concurrent-mode safe).                        //
// NOTE: useLayoutEffect is intentionally used here.                   //
// ------------------------------------------------------------------ //

export function useEffectTsValue<A, E>(
  factory: () => Effect.Effect<A, E, AppServices>,
  fallback: A,
  deps: DependencyList = []
): A {
  const runtime = useRuntime()
  const valueRef = useRef<A>(fallback)
  const listenersRef = useRef(new Set<() => void>())

  useLayoutEffect(() => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const effect = (factory() as Effect.Effect<A, E, any>).pipe(
      Effect.tap((value) =>
        Effect.sync(() => {
          valueRef.current = value
          listenersRef.current.forEach((fn) => fn())
        })
      )
    )
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const fiber = runtime.runFork(effect as any)
    return () => {
      void Effect.runFork(Fiber.interrupt(fiber))
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps)

  return useSyncExternalStore(
    (cb) => {
      listenersRef.current.add(cb)
      return () => {
        listenersRef.current.delete(cb)
      }
    },
    () => valueRef.current,
    () => fallback
  )
}

// ------------------------------------------------------------------ //
// useSubscriptionRef — subscribes to a SubscriptionRef's changes      //
// ------------------------------------------------------------------ //

export function useSubscriptionRef<A>(ref: SubscriptionRef.SubscriptionRef<A>, fallback: A): A {
  const runtime = useRuntime()
  const valueRef = useRef<A>(fallback)
  const listenersRef = useRef(new Set<() => void>())

  const subscribe = useCallback(
    (notify: () => void) => {
      listenersRef.current.add(notify)
      // Start streaming changes from the SubscriptionRef
      // In Effect 3.x, SubscriptionRef.SubscriptionRef<A> exposes `.changes` as a Stream<A>
      const streamEffect = ref.changes.pipe(
        Stream.runForEach((value: A) =>
          Effect.sync(() => {
            valueRef.current = value
            listenersRef.current.forEach((fn) => fn())
          })
        )
      )
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const fiber = runtime.runFork(streamEffect as any)
      return () => {
        listenersRef.current.delete(notify)
        Effect.runFork(Fiber.interrupt(fiber))
      }
    },
    [runtime, ref]
  )

  return useSyncExternalStore(
    subscribe,
    () => valueRef.current,
    () => fallback
  )
}
