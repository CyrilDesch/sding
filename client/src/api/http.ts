import type { ErrorResponse } from '../protocol/types'
import { ApiError, NetworkError, UnauthorizedError } from './errors'

const BASE = '/api'

async function rawFetch(url: string, options?: RequestInit): Promise<Response> {
  try {
    return await fetch(`${BASE}${url}`, options)
  } catch (e) {
    throw new NetworkError(String(e))
  }
}

async function readAndProcess<A>(response: Response, parse: (text: string) => A): Promise<A> {
  let text: string
  try {
    text = await response.text()
  } catch (e) {
    throw new NetworkError(String(e))
  }

  if (response.ok) {
    try {
      return parse(text)
    } catch (e) {
      throw new ApiError(`Parse error: ${String(e)}`, response.status)
    }
  }

  if (response.status === 401) {
    throw new UnauthorizedError()
  }

  const message = tryParseErrorMessage(text) ?? `HTTP ${response.status}`
  throw new ApiError(message, response.status)
}

function tryParseErrorMessage(text: string): string | null {
  try {
    const parsed = JSON.parse(text) as ErrorResponse
    return parsed.error ?? null
  } catch {
    return null
  }
}

const parseJson = <A>(text: string): A => JSON.parse(text) as A

export async function httpGet<A>(url: string): Promise<A> {
  const r = await rawFetch(url)
  return readAndProcess(r, parseJson<A>)
}

export async function httpPost<A>(url: string, body?: unknown): Promise<A> {
  const r = await rawFetch(url, {
    method: 'POST',
    headers: body !== undefined ? { 'Content-Type': 'application/json' } : undefined,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })
  return readAndProcess(r, parseJson<A>)
}

export async function httpPut<A>(url: string, body: unknown): Promise<A> {
  const r = await rawFetch(url, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  return readAndProcess(r, parseJson<A>)
}

export async function httpDelete<A>(url: string): Promise<A> {
  const r = await rawFetch(url, { method: 'DELETE' })
  return readAndProcess(r, parseJson<A>)
}
