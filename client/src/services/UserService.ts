import { Context, Effect, Layer } from 'effect'
import type { LlmConfigResponse, LlmProvider } from '../protocol/types'
import type { HttpError } from './http'
import { httpGet, httpPut } from './http'

// ─── Service interface ────────────────────────────────────────────────────────

export interface UserServiceShape {
  readonly getLlmConfig: Effect.Effect<LlmConfigResponse, HttpError>
  readonly saveLlmConfig: (
    provider: LlmProvider,
    apiKey: string,
    model: string
  ) => Effect.Effect<LlmConfigResponse, HttpError>
}

export class UserService extends Context.Tag('UserService')<UserService, UserServiceShape>() {}

// ─── Live implementation ─────────────────────────────────────────────────────

export const UserServiceLive = Layer.succeed(UserService, {
  getLlmConfig: httpGet<LlmConfigResponse>('/user/llm-config'),

  saveLlmConfig: (provider: LlmProvider, apiKey: string, model: string) =>
    httpPut<LlmConfigResponse>('/user/llm-config', { provider, apiKey, model }),
})
