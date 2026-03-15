import { describe, it, expect } from 'vitest'
import { Effect, SubscriptionRef } from 'effect'
import { AuthService, AuthServiceLive } from '../../src/services/AuthService'

describe('AuthService', () => {
  it('initial isLoggedIn state is false', async () => {
    const result = await Effect.runPromise(
      Effect.gen(function* () {
        const svc = yield* AuthService
        return yield* SubscriptionRef.get(svc.isLoggedIn)
      }).pipe(Effect.provide(AuthServiceLive))
    )
    expect(result).toBe(false)
  })

  it('markLoggedIn sets isLoggedIn to true', async () => {
    const result = await Effect.runPromise(
      Effect.gen(function* () {
        const svc = yield* AuthService
        yield* svc.markLoggedIn
        return yield* SubscriptionRef.get(svc.isLoggedIn)
      }).pipe(Effect.provide(AuthServiceLive))
    )
    expect(result).toBe(true)
  })

  it('markLoggedOut after markLoggedIn sets isLoggedIn back to false', async () => {
    const result = await Effect.runPromise(
      Effect.gen(function* () {
        const svc = yield* AuthService
        yield* svc.markLoggedIn
        yield* svc.markLoggedOut
        return yield* SubscriptionRef.get(svc.isLoggedIn)
      }).pipe(Effect.provide(AuthServiceLive))
    )
    expect(result).toBe(false)
  })
})
