import { useState } from 'react'
import { useAuthService } from '../hooks/useAuthService'
import { useRouterService } from '../hooks/useRouterService'

export function LoginPage() {
  const { login, markLoggedIn } = useAuthService()
  const { navigate } = useRouterService()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setIsLoading(true)
    setError(null)
    try {
      await login(email, password)
      markLoggedIn()
      navigate({ type: 'landing' })
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Login failed')
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-indigo-50 via-white to-purple-50 px-4">
      <div className="w-full max-w-md">
        <div className="mb-8 text-center">
          <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-indigo-600 shadow-lg">
            <span className="text-xl font-bold text-white">B</span>
          </div>
          <h1 className="text-3xl font-bold text-gray-900">Welcome back</h1>
          <p className="mt-2 text-gray-500">Sign in to your account</p>
        </div>

        <div className="rounded-2xl border border-gray-200 bg-white p-8 shadow-sm">
          <form onSubmit={handleSubmit}>
            <FormField
              label="Email"
              type="email"
              placeholder="email"
              value={email}
              onChange={setEmail}
            />
            <FormField
              label="Password"
              type="password"
              placeholder="password"
              value={password}
              onChange={setPassword}
            />
            {error && (
              <div className="mb-4 rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-600">
                {error}
              </div>
            )}
            <button
              type="submit"
              disabled={isLoading}
              className={`w-full rounded-xl py-3 text-sm font-semibold transition-all ${
                isLoading
                  ? 'cursor-wait bg-gray-400 text-white'
                  : 'bg-indigo-600 text-white shadow-sm hover:bg-indigo-700 hover:shadow'
              }`}
            >
              {isLoading ? 'Signing in...' : 'Sign in'}
            </button>
          </form>

          <div className="mt-6 text-center text-sm text-gray-500">
            {"Don't have an account? "}
            <a
              className="cursor-pointer font-medium text-indigo-600 hover:text-indigo-700"
              onClick={() => navigate({ type: 'register' })}
            >
              Sign up
            </a>
          </div>
        </div>
      </div>
    </div>
  )
}

interface FieldProps {
  label: string
  type: string
  placeholder: string
  value: string
  onChange: (v: string) => void
}

function FormField({ label, type, placeholder, value, onChange }: FieldProps) {
  return (
    <div className="mb-5">
      <label className="mb-1.5 block text-sm font-medium text-gray-700">{label}</label>
      <input
        className="w-full rounded-xl border border-gray-300 px-4 py-3 text-sm focus:border-transparent focus:ring-2 focus:ring-indigo-500 focus:outline-none"
        type={type}
        placeholder={placeholder}
        required
        value={value}
        onChange={(e) => onChange(e.target.value)}
      />
    </div>
  )
}
