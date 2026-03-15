import { httpPost } from './http'

export async function loginApi(email: string, password: string): Promise<void> {
  await httpPost('/auth/login', { email, password })
}

export async function registerApi(
  email: string,
  password: string,
  firstName: string,
  lastName: string
): Promise<void> {
  await httpPost('/auth/register', { email, password, firstName, lastName })
}

export async function logoutApi(): Promise<void> {
  try {
    await httpPost('/auth/logout')
  } catch {
    // best-effort — session cleanup continues regardless
  }
}

export async function checkAuthApi(): Promise<boolean> {
  try {
    const r = await fetch('/api/auth/me')
    return r.ok
  } catch {
    return false
  }
}

export const authQueryOptions = {
  queryKey: ['auth'] as const,
  queryFn: checkAuthApi,
  staleTime: Infinity,
  retry: false,
} as const
