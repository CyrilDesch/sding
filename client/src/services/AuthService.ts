import { Context, Effect, Layer, SubscriptionRef } from 'effect'
import type { HttpError } from './http'
import { httpPost } from './http'

// ─── Service interface ────────────────────────────────────────────────────────

export interface AuthServiceShape {
  readonly isLoggedIn: SubscriptionRef.SubscriptionRef<boolean>
  readonly markLoggedIn: Effect.Effect<void>
  readonly markLoggedOut: Effect.Effect<void>
  readonly checkAuth: Effect.Effect<boolean>
  readonly login: (email: string, password: string) => Effect.Effect<void, HttpError>
  readonly register: (
    email: string,
    password: string,
    firstName: string,
    lastName: string
  ) => Effect.Effect<void, HttpError>
  readonly logout: Effect.Effect<void>
}

export class AuthService extends Context.Tag('AuthService')<AuthService, AuthServiceShape>() {}

// ─── Live implementation ─────────────────────────────────────────────────────

export const AuthServiceLive = Layer.effect(
  AuthService,
  Effect.gen(function* () {
    const isLoggedInRef = yield* SubscriptionRef.make(false)

    const markLoggedOut = Effect.gen(function* () {
      yield* SubscriptionRef.set(isLoggedInRef, false)
      yield* Effect.sync(() => {
        window.location.hash = '#login'
      })
    })

    // Listen for 401 events dispatched by the HTTP layer (avoids circular dep).
    yield* Effect.sync(() => {
      window.addEventListener('sding:unauthorized', () => {
        Effect.runFork(SubscriptionRef.set(isLoggedInRef, false))
        window.location.hash = '#login'
      })
    })

    return {
      isLoggedIn: isLoggedInRef,

      markLoggedIn: SubscriptionRef.set(isLoggedInRef, true),

      markLoggedOut,

      // checkAuth always succeeds: returns true if server accepts the cookie.
      checkAuth: Effect.tryPromise({
        try: () => fetch('/api/auth/me').then((r) => r.ok),
        catch: () => new Error('checkAuth failed'),
      }).pipe(Effect.orElse(() => Effect.succeed(false))),

      login: (email: string, password: string) =>
        httpPost('/auth/login', { email, password }).pipe(Effect.asVoid),

      register: (email: string, password: string, firstName: string, lastName: string) =>
        httpPost('/auth/register', { email, password, firstName, lastName }).pipe(Effect.asVoid),

      logout: Effect.gen(function* () {
        yield* httpPost('/auth/logout').pipe(Effect.orElse(() => Effect.void))
        yield* markLoggedOut
      }),
    }
  })
)
