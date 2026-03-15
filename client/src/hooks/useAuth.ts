import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from '@tanstack/react-router'
import { authQueryOptions, loginApi, logoutApi, registerApi } from '../api/auth'

export function useAuth() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()

  const { data: isAuthenticated = false } = useQuery(authQueryOptions)

  const login = useMutation({
    mutationFn: ({ email, password }: { email: string; password: string }) =>
      loginApi(email, password),
    onSuccess: async () => {
      queryClient.setQueryData(authQueryOptions.queryKey, true)
      await navigate({ to: '/' })
    },
  })

  const register = useMutation({
    mutationFn: ({
      email,
      password,
      firstName,
      lastName,
    }: {
      email: string
      password: string
      firstName: string
      lastName: string
    }) => registerApi(email, password, firstName, lastName),
    onSuccess: async () => {
      queryClient.setQueryData(authQueryOptions.queryKey, true)
      await navigate({ to: '/settings' })
    },
  })

  const logout = useMutation({
    mutationFn: logoutApi,
    onSuccess: async () => {
      queryClient.setQueryData(authQueryOptions.queryKey, false)
      queryClient.clear()
      await navigate({ to: '/login' })
    },
  })

  return { isAuthenticated, login, register, logout }
}
