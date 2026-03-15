import { createContext, useContext } from 'react'
import type { AppRuntime } from './index'

export const RuntimeContext = createContext<AppRuntime | null>(null)

export function useRuntime(): AppRuntime {
  const ctx = useContext(RuntimeContext)
  if (!ctx) throw new Error('useRuntime must be used within a <RuntimeProvider>')
  return ctx
}
