import { randomUUID } from 'crypto'
import type { APIRequestContext } from '@playwright/test'

const API_URL = process.env.API_URL ?? 'http://localhost:8080'

export interface TestUser {
  email: string
  password: string
  cookieHeader: string
}

export async function createTestUser(request: APIRequestContext): Promise<TestUser> {
  const email = `test-${randomUUID()}@e2e.local`
  const password = 'Test1234!'

  const res = await request.post(`${API_URL}/api/auth/register`, {
    data: { email, password, firstName: 'E2E', lastName: 'User' },
  })

  if (!res.ok()) throw new Error(`Register failed: ${res.status()} ${await res.text()}`)

  const cookieValue = (res.headers()['set-cookie'] ?? '').split(';')[0] ?? ''
  return { email, password, cookieHeader: cookieValue }
}

export async function configureLocalLlm(
  request: APIRequestContext,
  cookieHeader: string,
  model = 'mlx-community/Qwen3.5-35B-A3B-4bit'
): Promise<void> {
  const res = await request.put(`${API_URL}/api/user/llm-config`, {
    headers: { Cookie: cookieHeader },
    data: { provider: 'Local', apiKey: '', model },
  })
  if (!res.ok()) throw new Error(`Configure LLM failed: ${res.status()} ${await res.text()}`)
}

export async function createChat(
  request: APIRequestContext,
  cookieHeader: string
): Promise<string> {
  const res = await request.post(`${API_URL}/api/chat`, {
    headers: { Cookie: cookieHeader },
  })
  if (!res.ok()) throw new Error(`Create chat failed: ${res.status()} ${await res.text()}`)
  const body = (await res.json()) as { chatId: string }
  return body.chatId
}

export async function submitInput(
  request: APIRequestContext,
  chatId: string,
  input: string
): Promise<void> {
  const res = await request.post(`${API_URL}/api/chat/${chatId}/input`, { data: { input } })
  if (!res.ok()) throw new Error(`Submit input failed: ${res.status()} ${await res.text()}`)
}

export async function getChatHistory(
  request: APIRequestContext,
  chatId: string
): Promise<{ events: unknown[]; liveIndex: number }> {
  const res = await request.get(`${API_URL}/api/chat/${chatId}/history`)
  if (!res.ok()) throw new Error(`Get history failed: ${res.status()} ${await res.text()}`)
  return res.json() as Promise<{ events: unknown[]; liveIndex: number }>
}

export { API_URL }
