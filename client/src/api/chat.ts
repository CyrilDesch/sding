import type { ChatHistoryResponse, ChatSummary, CreateChatResponse } from '../protocol/types'
import { httpDelete, httpGet, httpPost } from './http'

export async function listChatsApi(): Promise<ChatSummary[]> {
  const r = await httpGet<{ chats: ChatSummary[] }>('/chat')
  return r.chats
}

export async function createChatApi(): Promise<CreateChatResponse> {
  return httpPost<CreateChatResponse>('/chat')
}

export async function deleteChatApi(chatId: string): Promise<void> {
  await httpDelete(`/chat/${chatId}`)
}

export async function submitInputApi(chatId: string, input: string): Promise<void> {
  await httpPost(`/chat/${chatId}/input`, { input })
}

export async function getHistoryApi(chatId: string): Promise<ChatHistoryResponse> {
  return httpGet<ChatHistoryResponse>(`/chat/${chatId}/history`)
}
