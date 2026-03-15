import React, { useLayoutEffect, useMemo } from 'react'
import { createAppRuntime } from './index'
import { RuntimeContext } from './useRuntime'

// ------------------------------------------------------------------ //
// NOTE: useLayoutEffect is intentionally used here (and ONLY here)   //
// to dispose the ManagedRuntime on unmount. All other side effects    //
// must use useEffectTs from src/hooks/useEffectTs.                    //
// ------------------------------------------------------------------ //

export function RuntimeProvider({ children }: { children: React.ReactNode }) {
  const runtime = useMemo(() => createAppRuntime(), [])

  useLayoutEffect(() => {
    return () => {
      void runtime.dispose()
    }
  }, [runtime])

  return <RuntimeContext.Provider value={runtime}>{children}</RuntimeContext.Provider>
}
