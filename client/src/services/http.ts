import { Effect } from 'effect'
import type { ErrorResponse } from '../protocol/types'
import { ApiError, NetworkError, UnauthorizedError } from './errors'

export type HttpError = ApiError | UnauthorizedError | NetworkError

const BASE = '/api'

// ─── 401 global handler ──────────────────────────────────────────────────────

const dispatchUnauthorized = () => {
  window.dispatchEvent(new CustomEvent('sding:unauthorized'))
}

// ─── Internal helpers ─────────────────────────────────────────────────────────

const rawFetch = (url: string, options?: RequestInit): Effect.Effect<Response, NetworkError> =>
  Effect.tryPromise({
    try: () => fetch(`${BASE}${url}`, options),
    catch: (e) => new NetworkError(String(e)),
  })

function readAndProcess<A>(
  response: Response,
  parse: (text: string) => A
): Effect.Effect<A, HttpError> {
  return Effect.tryPromise<string, NetworkError>({
    try: () => response.text(),
    catch: (e) => new NetworkError(String(e)),
  }).pipe(
    Effect.flatMap((text): Effect.Effect<A, HttpError> => {
      if (response.ok) {
        return Effect.try<A, ApiError>({
          try: () => parse(text),
          catch: (e) => new ApiError(`Parse error: ${String(e)}`, response.status),
        })
      }
      if (response.status === 401) {
        dispatchUnauthorized()
        return Effect.fail(new UnauthorizedError())
      }
      const message = tryParseErrorMessage(text) ?? `HTTP ${response.status}`
      return Effect.fail(new ApiError(message, response.status))
    })
  )
}

function tryParseErrorMessage(text: string): string | null {
  try {
    const parsed = JSON.parse(text) as ErrorResponse
    return parsed.error ?? null
  } catch {
    return null
  }
}

// Default JSON parser with explicit cast so TypeScript stops complaining.
const parseJson = <A>(text: string): A => JSON.parse(text) as A

// ─── Public API ───────────────────────────────────────────────────────────────

export const httpGet = <A>(url: string): Effect.Effect<A, HttpError> =>
  rawFetch(url).pipe(Effect.flatMap((r) => readAndProcess(r, parseJson<A>)))

export const httpPost = <A>(url: string, body?: unknown): Effect.Effect<A, HttpError> =>
  rawFetch(url, {
    method: 'POST',
    headers: body !== undefined ? { 'Content-Type': 'application/json' } : undefined,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  }).pipe(Effect.flatMap((r) => readAndProcess(r, parseJson<A>)))

export const httpPut = <A>(url: string, body: unknown): Effect.Effect<A, HttpError> =>
  rawFetch(url, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  }).pipe(Effect.flatMap((r) => readAndProcess(r, parseJson<A>)))

export const httpDelete = <A>(url: string): Effect.Effect<A, HttpError> =>
  rawFetch(url, { method: 'DELETE' }).pipe(Effect.flatMap((r) => readAndProcess(r, parseJson<A>)))
