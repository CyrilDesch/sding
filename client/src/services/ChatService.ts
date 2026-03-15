import { Context, Effect, Layer } from 'effect'
import type { ChatHistoryResponse, ChatSummary, CreateChatResponse } from '../protocol/types'
import type { HttpError } from './http'
import { httpDelete, httpGet, httpPost } from './http'

// ─── Service interface ────────────────────────────────────────────────────────

export interface ChatServiceShape {
  readonly listChats: Effect.Effect<ChatSummary[], HttpError>
  readonly createChat: Effect.Effect<CreateChatResponse, HttpError>
  readonly deleteChat: (chatId: string) => Effect.Effect<void, HttpError>
  readonly submitInput: (chatId: string, input: string) => Effect.Effect<void, HttpError>
  readonly getHistory: (chatId: string) => Effect.Effect<ChatHistoryResponse, HttpError>
}

export class ChatService extends Context.Tag('ChatService')<ChatService, ChatServiceShape>() {}

// ─── Live implementation ─────────────────────────────────────────────────────

export const ChatServiceLive = Layer.succeed(ChatService, {
  listChats: httpGet<{ chats: ChatSummary[] }>('/chat').pipe(Effect.map((r) => r.chats)),

  createChat: httpPost<CreateChatResponse>('/chat'),

  deleteChat: (chatId: string) => httpDelete(`/chat/${chatId}`).pipe(Effect.asVoid),

  submitInput: (chatId: string, input: string) =>
    httpPost(`/chat/${chatId}/input`, { input }).pipe(Effect.asVoid),

  getHistory: (chatId: string) => httpGet<ChatHistoryResponse>(`/chat/${chatId}/history`),
})
