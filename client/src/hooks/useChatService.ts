import { Effect } from 'effect'
import { useCallback } from 'react'
import { ChatService } from '../services/ChatService'
import { useRuntime } from '../runtime/useRuntime'

export function useChatService() {
  const runtime = useRuntime()

  const listChats = useCallback(
    () => runtime.runPromise(ChatService.pipe(Effect.flatMap((svc) => svc.listChats))),
    [runtime]
  )

  const createChat = useCallback(
    () => runtime.runPromise(ChatService.pipe(Effect.flatMap((svc) => svc.createChat))),
    [runtime]
  )

  const deleteChat = useCallback(
    (chatId: string) =>
      runtime.runPromise(ChatService.pipe(Effect.flatMap((svc) => svc.deleteChat(chatId)))),
    [runtime]
  )

  const submitInput = useCallback(
    (chatId: string, input: string) =>
      runtime.runPromise(ChatService.pipe(Effect.flatMap((svc) => svc.submitInput(chatId, input)))),
    [runtime]
  )

  const getHistory = useCallback(
    (chatId: string) =>
      runtime.runPromise(ChatService.pipe(Effect.flatMap((svc) => svc.getHistory(chatId)))),
    [runtime]
  )

  return { listChats, createChat, deleteChat, submitInput, getHistory }
}
