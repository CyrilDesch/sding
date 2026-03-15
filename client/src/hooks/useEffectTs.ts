import { useLayoutEffect, type DependencyList } from 'react'
import { Effect, Fiber } from 'effect'
import { useRuntime } from '../runtime/useRuntime'
import type { AppServices } from '../runtime/AppLayer'

// ------------------------------------------------------------------ //
// useEffectTs — runs an Effect program, interrupts on unmount/re-run  //
//                                                                      //
// NOTE: useLayoutEffect is intentionally used here. This is the ONLY  //
// place (besides RuntimeContext) where useLayoutEffect is permitted.   //
// Feature components must use this hook instead of useEffect.         //
// ------------------------------------------------------------------ //

export function useEffectTs<A, E>(
  factory: () => Effect.Effect<A, E, AppServices>,
  deps: DependencyList = []
): void {
  const runtime = useRuntime()

  useLayoutEffect(() => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const fiber = runtime.runFork(factory() as any)
    return () => {
      void Effect.runFork(Fiber.interrupt(fiber))
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps)
}
