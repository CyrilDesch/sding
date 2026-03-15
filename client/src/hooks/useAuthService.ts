import { Effect } from 'effect'
import { useCallback, useMemo } from 'react'
import { AuthService } from '../services/AuthService'
import { useRuntime } from '../runtime/useRuntime'
import { useSubscriptionRef } from './useEffectTsValue'

export function useAuthService() {
  const runtime = useRuntime()

  const isLoggedInRef = useMemo(
    () => runtime.runSync(AuthService.pipe(Effect.map((svc) => svc.isLoggedIn))),
    [runtime]
  )
  const isLoggedIn = useSubscriptionRef(isLoggedInRef, false)

  const markLoggedIn = useCallback(
    () => runtime.runFork(AuthService.pipe(Effect.flatMap((svc) => svc.markLoggedIn))),
    [runtime]
  )

  const login = useCallback(
    (email: string, password: string) =>
      runtime.runPromise(AuthService.pipe(Effect.flatMap((svc) => svc.login(email, password)))),
    [runtime]
  )

  const register = useCallback(
    (email: string, password: string, firstName: string, lastName: string) =>
      runtime.runPromise(
        AuthService.pipe(
          Effect.flatMap((svc) => svc.register(email, password, firstName, lastName))
        )
      ),
    [runtime]
  )

  const logout = useCallback(
    () => runtime.runFork(AuthService.pipe(Effect.flatMap((svc) => svc.logout))),
    [runtime]
  )

  const checkAuth = useCallback(
    () => runtime.runPromise(AuthService.pipe(Effect.flatMap((svc) => svc.checkAuth))),
    [runtime]
  )

  return { isLoggedIn, markLoggedIn, login, register, logout, checkAuth }
}
