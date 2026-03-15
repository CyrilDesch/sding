import { useState } from 'react'
import { Effect } from 'effect'
import { ChatService } from '../services/ChatService'
import { useEffectTs } from '../hooks/useEffectTs'
import { useRouterService } from '../hooks/useRouterService'
import { useChatService } from '../hooks/useChatService'

export function LandingPage() {
  const { navigate } = useRouterService()
  const { createChat } = useChatService()
  const [isCreating, setIsCreating] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // If user already has chats, redirect to the most recent one.
  useEffectTs(
    () =>
      ChatService.pipe(
        Effect.flatMap((svc) => svc.listChats),
        Effect.tap((chats) =>
          chats.length > 0
            ? Effect.sync(() => navigate({ type: 'chat', chatId: chats[0]!.chatId }))
            : Effect.void
        ),
        Effect.catchAll(() => Effect.void)
      ),
    []
  )

  const handleStart = async () => {
    setIsCreating(true)
    setError(null)
    try {
      const resp = await createChat()
      navigate({ type: 'chat', chatId: resp.chatId })
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to start session')
      setIsCreating(false)
    }
  }

  return (
    <div className="flex min-h-[calc(100vh-3.5rem)] flex-1 items-center justify-center">
      <div className="mx-auto max-w-2xl px-6 text-center">
        <div className="mb-8">
          <img src="/logo.png" alt="Sding" className="mx-auto mb-6 h-20" />
          <h1 className="mb-4 text-5xl font-bold tracking-tight text-gray-900">sding</h1>
          <p className="mx-auto max-w-lg text-xl leading-relaxed text-gray-500">
            AI-powered product brainstorming. From problem discovery to validated prototypes.
          </p>
        </div>

        <div className="mb-12">
          <div className="mx-auto grid max-w-lg grid-cols-3 gap-6">
            <FeatureChip title="Problem Discovery" desc="Weird problems & trend analysis" />
            <FeatureChip title="User Research" desc="Empathy maps & JTBD" />
            <FeatureChip title="Solution Design" desc="SCAMPER & competitive analysis" />
          </div>
        </div>

        <button
          disabled={isCreating}
          className={`transform rounded-2xl px-8 py-4 text-lg font-semibold shadow-lg transition-all hover:scale-105 ${
            isCreating
              ? 'cursor-wait bg-gray-400 text-white'
              : 'bg-indigo-600 text-white hover:bg-indigo-700 hover:shadow-xl'
          }`}
          onClick={handleStart}
        >
          {isCreating ? 'Starting...' : 'Start Brainstorming'}
        </button>

        {error && <p className="mt-4 text-sm text-red-500">{error}</p>}
      </div>
    </div>
  )
}

function FeatureChip({ title, desc }: { title: string; desc: string }) {
  return (
    <div className="text-center">
      <p className="text-sm font-semibold text-gray-800">{title}</p>
      <p className="mt-1 text-xs text-gray-400">{desc}</p>
    </div>
  )
}
