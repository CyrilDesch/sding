import { useEffect } from 'react'
import { useNavigate } from '@tanstack/react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { createChatApi, listChatsApi } from '../api/chat'
import { errorMessage } from '../api/errors'

export function LandingPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const { data: chats } = useQuery({ queryKey: ['chats'], queryFn: listChatsApi })

  useEffect(() => {
    if (chats && chats.length > 0) {
      void navigate({ to: '/chat/$chatId', params: { chatId: chats[0]!.chatId } })
    }
  }, [chats, navigate])

  const createChat = useMutation({
    mutationFn: createChatApi,
    onSuccess: async (data) => {
      await queryClient.invalidateQueries({ queryKey: ['chats'] })
      await navigate({ to: '/chat/$chatId', params: { chatId: data.chatId } })
    },
    onError: (err) => toast.error(errorMessage(err)),
  })

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
          disabled={createChat.isPending}
          className={`transform rounded-2xl px-8 py-4 text-lg font-semibold shadow-lg transition-all hover:scale-105 ${
            createChat.isPending
              ? 'cursor-wait bg-gray-400 text-white'
              : 'bg-indigo-600 text-white hover:bg-indigo-700 hover:shadow-xl'
          }`}
          onClick={() => createChat.mutate()}
        >
          {createChat.isPending ? 'Starting...' : 'Start Brainstorming'}
        </button>
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
