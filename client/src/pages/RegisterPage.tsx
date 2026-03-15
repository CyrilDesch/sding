import { useState } from 'react'
import { useNavigate } from '@tanstack/react-router'
import { useAuth } from '../hooks/useAuth'
import { errorMessage } from '../api/errors'

export function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()

  const [firstName, setFirstName] = useState('')
  const [lastName, setLastName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    register.mutate({ email, password, firstName, lastName })
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-linear-to-br from-indigo-50 via-white to-purple-50 px-4">
      <div className="w-full max-w-md">
        <div className="mb-8 text-center">
          <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-indigo-600 shadow-lg">
            <span className="text-xl font-bold text-white">B</span>
          </div>
          <h1 className="text-3xl font-bold text-gray-900">Create account</h1>
          <p className="mt-2 text-gray-500">Start brainstorming in minutes</p>
        </div>

        <div className="rounded-2xl border border-gray-200 bg-white p-8 shadow-sm">
          <form onSubmit={handleSubmit}>
            <div className="grid grid-cols-2 gap-4">
              <FormField
                label="First name"
                type="text"
                placeholder="First name"
                value={firstName}
                onChange={setFirstName}
              />
              <FormField
                label="Last name"
                type="text"
                placeholder="Last name"
                value={lastName}
                onChange={setLastName}
              />
            </div>
            <FormField
              label="Email"
              type="email"
              placeholder="you@example.com"
              value={email}
              onChange={setEmail}
            />
            <FormField
              label="Password"
              type="password"
              placeholder="Min 8 characters"
              value={password}
              onChange={setPassword}
            />
            {register.isError && (
              <div className="mb-4 rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-600">
                {errorMessage(register.error)}
              </div>
            )}
            <button
              type="submit"
              disabled={register.isPending}
              className={`w-full rounded-xl py-3 text-sm font-semibold transition-all ${
                register.isPending
                  ? 'cursor-wait bg-gray-400 text-white'
                  : 'bg-indigo-600 text-white shadow-sm hover:bg-indigo-700 hover:shadow'
              }`}
            >
              {register.isPending ? 'Creating account...' : 'Create account'}
            </button>
          </form>

          <div className="mt-6 text-center text-sm text-gray-500">
            Already have an account?{' '}
            <a
              className="cursor-pointer font-medium text-indigo-600 hover:text-indigo-700"
              onClick={() => navigate({ to: '/login' })}
            >
              Sign in
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
