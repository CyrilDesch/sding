import { useState, useCallback } from 'react'
import { Effect } from 'effect'
import type { ChatSummary } from '../protocol/types'
import { ChatService } from '../services/ChatService'
import { useChatService } from '../hooks/useChatService'
import { useEffectTs } from '../hooks/useEffectTs'
import { useRouterService } from '../hooks/useRouterService'

interface Props {
  activeChatId: string
}

export function ConversationSidebar({ activeChatId }: Props) {
  const { deleteChat } = useChatService()
  const { navigate, currentPage } = useRouterService()
  const [chats, setChats] = useState<ChatSummary[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [deletingId, setDeletingId] = useState<string | null>(null)

  // Reload chat list whenever activeChatId changes (covers new session creation).
  useEffectTs(
    () =>
      activeChatId
        ? ChatService.pipe(
            Effect.flatMap((svc) => svc.listChats),
            Effect.tap((result) =>
              Effect.sync(() => {
                setChats(result)
                setIsLoading(false)
              })
            ),
            Effect.catchAll(() => Effect.sync(() => setIsLoading(false)))
          )
        : Effect.void,
    [activeChatId]
  )

  const handleDelete = useCallback(
    (chatId: string) => {
      setDeletingId(chatId)
      void deleteChat(chatId)
        .then(() => {
          setDeletingId(null)
          setChats((prev) => prev.filter((c) => c.chatId !== chatId))
          if (currentPage.type === 'chat' && currentPage.chatId === chatId) {
            navigate({ type: 'landing' })
          }
        })
        .catch(() => setDeletingId(null))
    },
    [deleteChat, currentPage, navigate]
  )

  return (
    <div className="flex w-64 flex-shrink-0 flex-col overflow-hidden border-r border-gray-200 bg-white">
      <div className="flex-shrink-0 border-b border-gray-200 px-4 py-3">
        <button
          className="flex w-full items-center justify-center gap-2 rounded-xl bg-indigo-600 px-3 py-2 text-sm font-medium text-white transition-colors hover:bg-indigo-700"
          onClick={() => navigate({ type: 'landing' })}
        >
          <span className="text-base leading-none font-bold">+</span>
          New session
        </button>
      </div>

      <div className="flex-1 overflow-y-auto py-2">
        {isLoading && <div className="px-4 py-8 text-center text-xs text-gray-400">Loading...</div>}
        {!isLoading && chats.length === 0 && (
          <div className="px-4 py-8 text-center text-xs text-gray-400">No sessions yet</div>
        )}
        {chats.map((chat) => {
          const isActive = chat.chatId === activeChatId
          const isDeleting = deletingId === chat.chatId

          const itemCls = isActive
            ? 'mx-2 mb-1 px-3 py-2.5 rounded-lg transition-colors group relative flex items-start gap-2 bg-indigo-50 border border-indigo-200'
            : 'mx-2 mb-1 px-3 py-2.5 rounded-lg transition-colors group relative flex items-start gap-2 hover:bg-gray-100 cursor-pointer'

          return (
            <div key={chat.chatId} className={itemCls}>
              <div
                className="min-w-0 flex-1"
                onClick={() => navigate({ type: 'chat', chatId: chat.chatId })}
              >
                <p
                  className={`truncate text-sm font-medium ${isActive ? 'text-indigo-700' : 'text-gray-700'}`}
                >
                  {chat.title}
                </p>
                <p className="mt-0.5 truncate font-mono text-xs text-gray-400">
                  {chat.chatId.slice(0, 8)}
                </p>
              </div>
              <button
                className={`flex h-6 w-6 flex-shrink-0 items-center justify-center rounded text-xs text-gray-300 opacity-0 transition-all group-hover:opacity-100 hover:bg-red-50 hover:text-red-500 leading-none${isDeleting ? 'text-red-400 opacity-100' : ''}`}
                disabled={isDeleting}
                onClick={(e) => {
                  e.stopPropagation()
                  handleDelete(chat.chatId)
                }}
              >
                ✕
              </button>
            </div>
          )
        })}
      </div>
    </div>
  )
}
