import type { LlmConfigResponse, LlmProvider } from '../protocol/types'
import { httpGet, httpPut } from './http'

export async function getLlmConfigApi(): Promise<LlmConfigResponse> {
  return httpGet<LlmConfigResponse>('/user/llm-config')
}

export async function saveLlmConfigApi(
  provider: LlmProvider,
  apiKey: string,
  model: string
): Promise<LlmConfigResponse> {
  return httpPut<LlmConfigResponse>('/user/llm-config', { provider, apiKey, model })
}
