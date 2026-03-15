// ─── Workflow Steps ──────────────────────────────────────────────────────────

export type WorkflowStep =
  | 'human_requirements'
  | 'weird_problem_generation'
  | 'problem_reformulation'
  | 'trend_analysis'
  | 'problem_selection'
  | 'human_problem_selection'
  | 'user_interviews'
  | 'empathy_map'
  | 'jtbd_definition'
  | 'human_jtbd_selection'
  | 'hmw'
  | 'crazy8s'
  | 'scamper'
  | 'competitive_analysis'
  | 'human_comprehensive_selection'
  | 'prototype_builds'
  | 'human_project_selection'
  | 'premium_report'
  | 'markdown_generation'

export const WORKFLOW_STEP_NAMES: Record<WorkflowStep, string> = {
  human_requirements: 'Your Requirements',
  weird_problem_generation: 'Problem Discovery',
  problem_reformulation: 'Problem Refinement',
  trend_analysis: 'Trend Analysis',
  problem_selection: 'Problem Selection',
  human_problem_selection: 'Choose a Problem',
  user_interviews: 'User Interviews',
  empathy_map: 'Empathy Map',
  jtbd_definition: 'Jobs to Be Done',
  human_jtbd_selection: 'Choose Key Job',
  hmw: 'How Might We?',
  crazy8s: 'Crazy 8s Ideas',
  scamper: 'SCAMPER Analysis',
  competitive_analysis: 'Competitive Analysis',
  human_comprehensive_selection: 'Select Best Idea',
  prototype_builds: 'Build Prototypes',
  human_project_selection: 'Choose Your Project',
  premium_report: 'Premium Report',
  markdown_generation: 'Finalize Report',
}

// ─── LLM Provider ────────────────────────────────────────────────────────────

export type LlmProvider = 'Gemini' | 'OpenAI' | 'Anthropic' | 'Local'

export interface LlmConfigResponse {
  provider: LlmProvider
  model: string
  keyHint: string
}

export interface LlmConfigRequest {
  provider: LlmProvider
  apiKey: string
  model: string
}

// ─── Selection types ─────────────────────────────────────────────────────────

export interface SelectionDetail {
  label: string
  value: string
}

export interface SelectionItem {
  id: string
  label: string
  description: string | null
  score: number | null
  tags: string[]
  details: SelectionDetail[]
}

// ─── SSE Events ──────────────────────────────────────────────────────────────

export interface SseWorkflowPlan {
  type: 'workflow_plan'
  steps: WorkflowStep[]
}

export interface SseMessage {
  type: 'message'
  content: string
  format: string
  source_node: WorkflowStep | null
}

export interface SseUserMessage {
  type: 'user_message'
  content: string
}

export interface SseStateUpdate {
  type: 'state'
  content: string
  source_node: WorkflowStep | null
}

export interface SseInputRequest {
  type: 'input_request'
  prompt: string
  options: string[] | null
  source_node: WorkflowStep | null
}

export interface SseSelectionRequest {
  type: 'selection_request'
  title: string
  items: SelectionItem[]
  allow_retry: boolean
  source_node: WorkflowStep | null
}

export interface SseNodeComplete {
  type: 'node_complete'
  node: WorkflowStep
}

export interface SseWorkflowComplete {
  type: 'workflow_complete'
  chat_id: string
}

export interface SseError {
  type: 'error'
  message: string
}

export type SseEvent =
  | SseWorkflowPlan
  | SseMessage
  | SseUserMessage
  | SseStateUpdate
  | SseInputRequest
  | SseSelectionRequest
  | SseNodeComplete
  | SseWorkflowComplete
  | SseError

export const SSE_EVENT_TYPES = [
  'workflow_plan',
  'message',
  'user_message',
  'state',
  'input_request',
  'selection_request',
  'node_complete',
  'workflow_complete',
  'error',
] as const

export type SseEventType = (typeof SSE_EVENT_TYPES)[number]

// ─── API Models ───────────────────────────────────────────────────────────────

export interface CreateChatResponse {
  chatId: string
}

export interface ChatSummary {
  chatId: string
  title: string
}

export interface ListChatsResponse {
  chats: ChatSummary[]
}

export interface ChatHistoryResponse {
  events: SseEvent[]
  liveIndex: number
}

export interface StatusResponse {
  status: string
}

export interface ErrorResponse {
  error: string
}

// ─── Router ───────────────────────────────────────────────────────────────────

export type Page =
  | { type: 'landing' }
  | { type: 'login' }
  | { type: 'register' }
  | { type: 'settings' }
  | { type: 'chat'; chatId: string }

export function parsePage(hash: string): Page {
  const h = hash.startsWith('#') ? hash.slice(1) : hash
  if (h === 'login') return { type: 'login' }
  if (h === 'register') return { type: 'register' }
  if (h === 'settings') return { type: 'settings' }
  if (h.startsWith('chat/')) {
    const chatId = h.slice(5)
    if (chatId) return { type: 'chat', chatId }
  }
  return { type: 'landing' }
}

export function toHash(page: Page): string {
  switch (page.type) {
    case 'landing':
      return ''
    case 'login':
      return '#login'
    case 'register':
      return '#register'
    case 'settings':
      return '#settings'
    case 'chat':
      return `#chat/${page.chatId}`
  }
}

// ─── Toast ────────────────────────────────────────────────────────────────────

export type ToastVariant = 'success' | 'error' | 'info'

export interface ToastMessage {
  id: number
  message: string
  variant: ToastVariant
}
