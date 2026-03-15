import { useState } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { toast } from 'sonner'
import type { LlmConfigResponse, LlmProvider } from '../protocol/types'
import { getLlmConfigApi, saveLlmConfigApi } from '../api/user'
import { errorMessage } from '../api/errors'

const DEFAULT_MODELS: Record<LlmProvider, string> = {
  Gemini: 'gemini-2.0-flash',
  OpenAI: 'gpt-4o',
  Anthropic: 'claude-sonnet-4-20250514',
  DeepSeek: 'deepseek-chat',
  OpenRouter: 'deepseek/deepseek-v3.2',
}

const PROVIDERS: LlmProvider[] = ['Gemini', 'OpenAI', 'Anthropic', 'DeepSeek', 'OpenRouter']

export function SettingsPage() {
  const { data: config, isLoading } = useQuery({
    queryKey: ['llm-config'],
    queryFn: getLlmConfigApi,
  })

  return (
    <div className="mx-auto max-w-2xl px-6 py-10">
      <h1 className="mb-2 text-2xl font-bold text-gray-900">Settings</h1>
      <p className="mb-8 text-gray-500">Configure your LLM provider to start brainstorming.</p>

      <div className="rounded-2xl border border-gray-200 bg-white p-8 shadow-sm">
        {isLoading || !config ? (
          <div className="py-8 text-center text-gray-400">Loading configuration...</div>
        ) : (
          // Mount form only after data is ready so useState initialises from props, not effects.
          <SettingsForm key={config.provider + config.model} initialConfig={config} />
        )}
      </div>
    </div>
  )
}

// ─── Form ─────────────────────────────────────────────────────────────────────

interface FormProps {
  initialConfig: LlmConfigResponse
}

function SettingsForm({ initialConfig }: FormProps) {
  const [provider, setProvider] = useState<LlmProvider>(initialConfig.provider)
  const [apiKey, setApiKey] = useState('')
  const [model, setModel] = useState(initialConfig.model)
  const [keyHint, setKeyHint] = useState<string | null>(initialConfig.keyHint)

  const save = useMutation({
    mutationFn: ({ p, k, m }: { p: LlmProvider; k: string; m: string }) =>
      saveLlmConfigApi(p, k, m),
    onSuccess: (data) => {
      setKeyHint(data.keyHint)
      setApiKey('')
      toast.success('Configuration saved successfully!')
    },
    onError: (err) => toast.error(errorMessage(err)),
  })

  const handleProviderChange = (p: LlmProvider) => {
    setProvider(p)
    setModel(DEFAULT_MODELS[p] ?? '')
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    const m = model.trim() || (DEFAULT_MODELS[provider] ?? '')
    save.mutate({ p: provider, k: apiKey, m })
  }

  return (
    <form onSubmit={handleSubmit}>
      <div className="mb-6">
        <label className="mb-2 block text-sm font-medium text-gray-700">LLM Provider</label>
        <div className="grid grid-cols-3 gap-3 sm:grid-cols-5">
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

      <button
        type="submit"
        disabled={save.isPending}
        className={`w-full rounded-xl py-3 text-sm font-semibold transition-all ${
          save.isPending
            ? 'cursor-wait bg-gray-400 text-white'
            : 'bg-indigo-600 text-white shadow-sm hover:bg-indigo-700 hover:shadow'
        }`}
      >
        {save.isPending ? 'Saving...' : 'Save Configuration'}
      </button>
    </form>
  )
}
