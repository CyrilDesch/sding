import { createFileRoute, Outlet, redirect } from '@tanstack/react-router'
import { authQueryOptions } from '../api/auth'
import { Navbar } from '../components/Navbar'

export const Route = createFileRoute('/_app')({
  beforeLoad: async ({ context }) => {
    const isAuth = await context.queryClient.ensureQueryData(authQueryOptions)
    if (!isAuth) throw redirect({ to: '/login' })
  },
  component: function AppLayout() {
    return (
      <div className="flex min-h-screen flex-col">
        <Navbar />
        <Outlet />
      </div>
    )
  },
})
