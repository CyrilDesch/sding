import { useAuthService } from '../hooks/useAuthService'
import { useRouterService } from '../hooks/useRouterService'

export function Navbar() {
  const { isLoggedIn, logout } = useAuthService()
  const { navigate } = useRouterService()

  return (
    <nav className="border-b border-gray-200 bg-white">
      <div className="mx-auto flex h-14 max-w-7xl items-center justify-between px-6">
        <div className="flex items-center gap-6">
          <a
            className="flex cursor-pointer items-center gap-2"
            onClick={() => navigate({ type: 'landing' })}
          >
            <img src="/logo.png" alt="Sding" className="h-8" />
            <span className="text-base font-semibold text-gray-900">sding</span>
          </a>
          {isLoggedIn && (
            <div className="flex items-center gap-4">
              <NavLink label="New session" onClick={() => navigate({ type: 'landing' })} />
              <NavLink label="Settings" onClick={() => navigate({ type: 'settings' })} />
            </div>
          )}
        </div>

        {isLoggedIn ? (
          <button
            className="text-sm text-gray-500 transition-colors hover:text-gray-700"
            onClick={() => logout()}
          >
            Sign out
          </button>
        ) : (
          <div className="flex items-center gap-3">
            <a
              className="cursor-pointer text-sm text-gray-500 transition-colors hover:text-gray-700"
              onClick={() => navigate({ type: 'login' })}
            >
              Sign in
            </a>
            <a
              className="cursor-pointer rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-indigo-700"
              onClick={() => navigate({ type: 'register' })}
            >
              Sign up
            </a>
          </div>
        )}
      </div>
    </nav>
  )
}

function NavLink({ label, onClick }: { label: string; onClick: () => void }) {
  return (
    <a
      className="cursor-pointer text-sm text-gray-500 transition-colors hover:text-gray-700"
      onClick={onClick}
    >
      {label}
    </a>
  )
}
