import { Effect } from 'effect'
import { useCallback } from 'react'
import type { LlmProvider } from '../protocol/types'
import { UserService } from '../services/UserService'
import { useRuntime } from '../runtime/useRuntime'

export function useUserService() {
  const runtime = useRuntime()

  const getLlmConfig = useCallback(
    () => runtime.runPromise(UserService.pipe(Effect.flatMap((svc) => svc.getLlmConfig))),
    [runtime]
  )

  const saveLlmConfig = useCallback(
    (provider: LlmProvider, apiKey: string, model: string) =>
      runtime.runPromise(
        UserService.pipe(Effect.flatMap((svc) => svc.saveLlmConfig(provider, apiKey, model)))
      ),
    [runtime]
  )

  return { getLlmConfig, saveLlmConfig }
}
