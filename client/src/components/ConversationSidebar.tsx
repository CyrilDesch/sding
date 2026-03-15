import { useCallback } from 'react'
import { useNavigate, useRouterState } from '@tanstack/react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { ChatSummary } from '../protocol/types'
import { createChatApi, deleteChatApi, listChatsApi } from '../api/chat'

interface Props {
  activeChatId: string
}

export function ConversationSidebar({ activeChatId }: Props) {
  const navigate = useNavigate()
  const routerState = useRouterState()
  const queryClient = useQueryClient()

  const { data: chats = [], isLoading } = useQuery({
    queryKey: ['chats'],
    queryFn: listChatsApi,
  })

  const createChat = useMutation({
    mutationFn: createChatApi,
    onSuccess: async (data) => {
      await queryClient.invalidateQueries({ queryKey: ['chats'] })
      await navigate({ to: '/chat/$chatId', params: { chatId: data.chatId } })
    },
  })

  const deleteChat = useMutation({
    mutationFn: (chatId: string) => deleteChatApi(chatId),
    onSuccess: async (_, chatId) => {
      await queryClient.invalidateQueries({ queryKey: ['chats'] })
      const currentPath = routerState.location.pathname
      if (currentPath === `/chat/${chatId}`) {
        await navigate({ to: '/' })
      }
    },
  })

  const handleDelete = useCallback(
    (chatId: string) => {
      deleteChat.mutate(chatId)
    },
    [deleteChat]
  )

  return (
    <div className="flex w-64 shrink-0 flex-col overflow-hidden border-r border-gray-200 bg-white">
      <div className="shrink-0 border-b border-gray-200 px-4 py-3">
        <button
          className="flex w-full items-center justify-center gap-2 rounded-xl bg-indigo-600 px-3 py-2 text-sm font-medium text-white transition-colors hover:bg-indigo-700"
          disabled={createChat.isPending}
          onClick={() => createChat.mutate()}
        >
          <span className="text-base leading-none font-bold">+</span>
          New session
        </button>
      </div>

      <div className="flex-1 overflow-y-auto py-2">
        {isLoading && (
          <div className="px-4 py-8 text-center text-xs text-gray-400">Loading...</div>
        )}
        {!isLoading && chats.length === 0 && (
          <div className="px-4 py-8 text-center text-xs text-gray-400">No sessions yet</div>
        )}
        {chats.map((chat: ChatSummary) => (
          <ChatItem
            key={chat.chatId}
            chat={chat}
            isActive={chat.chatId === activeChatId}
            isDeleting={deleteChat.isPending && deleteChat.variables === chat.chatId}
            onNavigate={() => navigate({ to: '/chat/$chatId', params: () => ({ chatId: chat.chatId }) })}
            onDelete={() => handleDelete(chat.chatId)}
          />
        ))}
      </div>
    </div>
  )
}

interface ChatItemProps {
  chat: ChatSummary
  isActive: boolean
  isDeleting: boolean
  onNavigate: () => void
  onDelete: () => void
}

function ChatItem({ chat, isActive, isDeleting, onNavigate, onDelete }: ChatItemProps) {
  const itemCls = isActive
    ? 'mx-2 mb-1 px-3 py-2.5 rounded-lg transition-colors group relative flex items-start gap-2 bg-indigo-50 border border-indigo-200'
    : 'mx-2 mb-1 px-3 py-2.5 rounded-lg transition-colors group relative flex items-start gap-2 hover:bg-gray-100 cursor-pointer'

  return (
    <div className={itemCls}>
      <div className="min-w-0 flex-1" onClick={onNavigate}>
        <p
          className={`truncate text-sm font-medium ${isActive ? 'text-indigo-700' : 'text-gray-700'}`}
        >
          {chat.title}
        </p>
        <p className="mt-0.5 truncate font-mono text-xs text-gray-400">{chat.chatId.slice(0, 8)}</p>
      </div>
      <button
        className={`flex h-6 w-6 shrink-0 items-center justify-center rounded text-xs text-gray-300 opacity-0 transition-all group-hover:opacity-100 hover:bg-red-50 hover:text-red-500 leading-none${isDeleting ? 'text-red-400 opacity-100' : ''}`}
        disabled={isDeleting}
        onClick={(e) => {
          e.stopPropagation()
          onDelete()
        }}
      >
        ✕
      </button>
    </div>
  )
}
