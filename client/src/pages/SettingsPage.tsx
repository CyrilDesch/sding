import { useState } from 'react'
import { Effect } from 'effect'
import type { LlmProvider } from '../protocol/types'
import { UserService } from '../services/UserService'
import { useEffectTs } from '../hooks/useEffectTs'
import { useUserService } from '../hooks/useUserService'
import { Navbar } from '../components/Navbar'

const DEFAULT_MODELS: Record<string, string> = {
  Gemini: 'gemini-2.0-flash',
  OpenAI: 'gpt-4o',
  Anthropic: 'claude-sonnet-4-20250514',
}

const PROVIDERS: LlmProvider[] = ['Gemini', 'OpenAI', 'Anthropic']

export function SettingsPage() {
  const { saveLlmConfig } = useUserService()

  const [provider, setProvider] = useState<LlmProvider>('Gemini')
  const [apiKey, setApiKey] = useState('')
  const [model, setModel] = useState(DEFAULT_MODELS['Gemini']!)
  const [keyHint, setKeyHint] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  // Load existing config on mount.
  useEffectTs(
    () =>
      UserService.pipe(
        Effect.flatMap((svc) => svc.getLlmConfig),
        Effect.tap((cfg) =>
          Effect.sync(() => {
            setProvider(cfg.provider)
            setModel(cfg.model)
            setKeyHint(cfg.keyHint)
            setIsLoading(false)
          })
        ),
        Effect.catchAll(() => Effect.sync(() => setIsLoading(false)))
      ),
    []
  )

  const handleProviderChange = (p: LlmProvider) => {
    setProvider(p)
    setModel(DEFAULT_MODELS[p] ?? '')
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setIsSaving(true)
    setError(null)
    setSuccess(null)
    const mdl = model.trim() || (DEFAULT_MODELS[provider] ?? '')
    try {
      const resp = await saveLlmConfig(provider, apiKey, mdl)
      setKeyHint(resp.keyHint)
      setApiKey('')
      setSuccess('Configuration saved successfully!')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save')
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />
      <div className="mx-auto max-w-2xl px-6 py-10">
        <h1 className="mb-2 text-2xl font-bold text-gray-900">Settings</h1>
        <p className="mb-8 text-gray-500">Configure your LLM provider to start brainstorming.</p>

        <div className="rounded-2xl border border-gray-200 bg-white p-8 shadow-sm">
          {isLoading ? (
            <div className="py-8 text-center text-gray-400">Loading configuration...</div>
          ) : (
            <form onSubmit={handleSubmit}>
              {/* Provider selector */}
              <div className="mb-6">
                <label className="mb-2 block text-sm font-medium text-gray-700">LLM Provider</label>
                <div className="grid grid-cols-3 gap-3">
                  {PROVIDERS.map((p) => (
                    <button
                      key={p}
                      type="button"
                      className={`rounded-xl border-2 px-4 py-3 text-sm font-medium transition-all ${
                        provider === p
                          ? 'border-indigo-500 bg-indigo-50 text-indigo-700'
                          : 'border-gray-200 bg-white text-gray-600 hover:border-gray-300'
                      }`}
                      onClick={() => handleProviderChange(p)}
                    >
                      {p}
                    </button>
                  ))}
                </div>
              </div>

              {/* API Key */}
              <div className="mb-6">
                <label className="mb-1.5 block text-sm font-medium text-gray-700">API Key</label>
                {keyHint && <p className="mb-2 text-xs text-gray-400">Current key: {keyHint}</p>}
                <input
                  className="w-full rounded-xl border border-gray-300 px-4 py-3 font-mono text-sm focus:border-transparent focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                  type="password"
                  placeholder="Enter your API key"
                  required
                  value={apiKey}
                  onChange={(e) => setApiKey(e.target.value)}
                />
              </div>

              {/* Model */}
              <div className="mb-6">
                <label className="mb-1.5 block text-sm font-medium text-gray-700">Model</label>
                <input
                  className="w-full rounded-xl border border-gray-300 px-4 py-3 text-sm focus:border-transparent focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                  placeholder="e.g. gemini-2.0-flash"
                  value={model}
                  onChange={(e) => setModel(e.target.value)}
                />
                <p className="mt-1.5 text-xs text-gray-400">
                  Leave empty for the default model of the selected provider.
                </p>
              </div>

              {error && (
                <div className="mb-4 rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-600">
                  {error}
                </div>
              )}
              {success && (
                <div className="mb-4 rounded-lg border border-green-200 bg-green-50 p-3 text-sm text-green-600">
                  {success}
                </div>
              )}

              <button
                type="submit"
                disabled={isSaving}
                className={`w-full rounded-xl py-3 text-sm font-semibold transition-all ${
                  isSaving
                    ? 'cursor-wait bg-gray-400 text-white'
                    : 'bg-indigo-600 text-white shadow-sm hover:bg-indigo-700 hover:shadow'
                }`}
              >
                {isSaving ? 'Saving...' : 'Save Configuration'}
              </button>
            </form>
          )}
        </div>
      </div>
    </div>
  )
}
