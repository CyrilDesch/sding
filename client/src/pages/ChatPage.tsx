import { useState, useRef, useCallback } from 'react'
import { Effect, Stream } from 'effect'
import type {
  SseEvent,
  SseInputRequest,
  SseSelectionRequest,
  WorkflowStep,
} from '../protocol/types'
import { SseService } from '../services/SseService'
import { ChatService } from '../services/ChatService'
import { useEffectTs } from '../hooks/useEffectTs'
import { useChatService } from '../hooks/useChatService'
import { useToastService } from '../hooks/useToastService'
import { ConversationSidebar } from '../components/ConversationSidebar'
import { AgentMessage, ErrorMessage, StateMessage, UserMessage } from '../components/MessageBubble'
import { ProgressBar } from '../components/ProgressBar'
import { NodeCompleteCard, WorkflowCompleteCard } from '../components/ResultCard'
import { ChoiceInputForm, TextInputForm } from '../components/InputForm'
import { SelectionPanel } from '../components/SelectionPanel'

// ─── Chat item types ──────────────────────────────────────────────────────────

type ChatItem =
  | { kind: 'agent'; content: string; node: WorkflowStep | null }
  | { kind: 'user'; content: string }
  | { kind: 'state'; content: string }
  | { kind: 'nodeDone'; step: WorkflowStep }
  | { kind: 'done'; chatId: string }
  | { kind: 'error'; message: string }

// ─── Component state ──────────────────────────────────────────────────────────

interface ChatState {
  plan: WorkflowStep[]
  items: ChatItem[]
  completed: Set<WorkflowStep>
  activeNode: WorkflowStep | null
  pendingInput: SseInputRequest | null
  pendingSelection: SseSelectionRequest | null
  reportContent: string | null
  liveIndex: number
}

const INITIAL_STATE: ChatState = {
  plan: [],
  items: [],
  completed: new Set(),
  activeNode: null,
  pendingInput: null,
  pendingSelection: null,
  reportContent: null,
  liveIndex: 0,
}

// ─── ChatPage ─────────────────────────────────────────────────────────────────

interface Props {
  chatId: string
}

export function ChatPage({ chatId }: Props) {
  const { submitInput } = useChatService()
  const { show: showToast } = useToastService()
  const [state, setState] = useState<ChatState>(INITIAL_STATE)
  const [retryCount, setRetryCount] = useState(0)

  // Keep processEvent stable — use ref so the SSE fiber always calls the latest version.
  const processEventRef = useRef<(event: SseEvent) => void>(() => undefined)

  processEventRef.current = useCallback(
    (event: SseEvent) => {
      switch (event.type) {
        case 'workflow_plan':
          setState((prev) => ({ ...prev, plan: event.steps }))
          break

        case 'message':
          setState((prev) => {
            const newReport =
              event.source_node === 'markdown_generation'
                ? (prev.reportContent ?? '') + '\n' + event.content
                : prev.reportContent
            return {
              ...prev,
              items: [
                ...prev.items,
                { kind: 'agent', content: event.content, node: event.source_node },
              ],
              reportContent: newReport,
            }
          })
          scrollToBottom()
          break

        case 'user_message':
          setState((prev) => ({
            ...prev,
            items: [...prev.items, { kind: 'user', content: event.content }],
          }))
          scrollToBottom()
          break

        case 'state':
          setState((prev) => ({
            ...prev,
            items: [...prev.items, { kind: 'state', content: event.content }],
            activeNode: event.source_node,
          }))
          scrollToBottom()
          break

        case 'input_request':
          setState((prev) => ({ ...prev, pendingSelection: null, pendingInput: event }))
          scrollToBottom()
          break

        case 'selection_request':
          setState((prev) => ({ ...prev, pendingInput: null, pendingSelection: event }))
          scrollToBottom()
          break

        case 'node_complete':
          setState((prev) => ({
            ...prev,
            pendingInput: null,
            pendingSelection: null,
            completed: new Set([...prev.completed, event.node]),
            activeNode: null,
            items: [...prev.items, { kind: 'nodeDone', step: event.node }],
          }))
          scrollToBottom()
          break

        case 'workflow_complete':
          setState((prev) => ({
            ...prev,
            activeNode: null,
            pendingInput: null,
            pendingSelection: null,
            items: [...prev.items, { kind: 'done', chatId: event.chat_id }],
          }))
          scrollToBottom()
          break

        case 'error':
          setState((prev) => ({
            ...prev,
            items: [...prev.items, { kind: 'error', message: event.message }],
          }))
          scrollToBottom()
          break
      }
    },
    // No deps — processEventRef.current is the always-up-to-date version.
    []
  )

  // Load history then stream SSE. Interrupts (and reconnects) whenever chatId or retryCount changes.
  useEffectTs(
    () =>
      Effect.gen(function* () {
        // Reset state for the new chat.
        yield* Effect.sync(() => setState({ ...INITIAL_STATE }))

        // Load persisted history.
        const history = yield* ChatService.pipe(
          Effect.flatMap((svc) => svc.getHistory(chatId)),
          Effect.catchAll((err) => {
            const msg = 'message' in err ? String(err.message) : 'Failed to load history'
            return Effect.sync(() =>
              setState((prev) => ({
                ...prev,
                items: [{ kind: 'error', message: `Failed to load conversation: ${msg}` }],
              }))
            ).pipe(Effect.flatMap(() => Effect.fail(err)))
          })
        )

        // Replay history events.
        yield* Effect.sync(() => {
          history.events.forEach((ev) => processEventRef.current(ev))
        })

        // Check if workflow is already complete before starting SSE.
        const isComplete = history.events.some((ev) => ev.type === 'workflow_complete')
        if (isComplete) return

        // Connect SSE stream from liveIndex.
        yield* SseService.pipe(
          Effect.flatMap((svc) =>
            svc
              .connect(chatId, history.liveIndex)
              .pipe(Stream.runForEach((event) => Effect.sync(() => processEventRef.current(event))))
          )
        )
      }),
    [chatId, retryCount]
  )

  const handleRetry = useCallback(() => setRetryCount((c) => c + 1), [])

  const handleAnswer = useCallback(
    (answer: string) => {
      const savedPending = state.pendingInput
      setState((prev) => ({
        ...prev,
        pendingInput: null,
        items: [...prev.items, { kind: 'user', content: answer }],
      }))
      void submitInput(chatId, answer).catch(() => {
        showToast('Failed to send your message', 'error')
        setState((prev) => ({ ...prev, pendingInput: savedPending }))
      })
    },
    [chatId, submitInput, showToast, state.pendingInput]
  )

  const handleSelectionAnswer = useCallback(
    (id: string) => {
      setState((prev) => ({ ...prev, pendingSelection: null }))
      void submitInput(chatId, id).catch(() => {
        showToast('Failed to send your selection', 'error')
      })
    },
    [chatId, submitInput, showToast]
  )

  return (
    <div className="flex bg-gray-50" style={{ height: 'calc(100vh - 3.5rem)' }}>
      <ConversationSidebar activeChatId={chatId} />

      <div className="flex min-w-0 flex-1 flex-col">
        {/* Header */}
        <div className="flex flex-shrink-0 items-center justify-between border-b border-gray-200 bg-white px-6 py-4">
          <h2 className="text-lg font-semibold text-gray-800">Brainstorming Session</h2>
          <span className="font-mono text-xs text-gray-400">{chatId.slice(0, 8)}</span>
        </div>

        {/* Messages */}
        <div className="chat-scroll flex-1 overflow-y-auto px-6 py-4">
          {state.items.map((item, i) => {
            switch (item.kind) {
              case 'agent':
                return <AgentMessage key={i} content={item.content} sourceNode={item.node} />
              case 'user':
                return <UserMessage key={i} content={item.content} />
              case 'state':
                return <StateMessage key={i} content={item.content} />
              case 'nodeDone':
                return <NodeCompleteCard key={i} step={item.step} />
              case 'done':
                return <WorkflowCompleteCard key={i} reportContent={state.reportContent} />
              case 'error':
                return <ErrorMessage key={i} message={item.message} onRetry={handleRetry} />
            }
          })}
        </div>

        {/* Selection panel */}
        {state.pendingSelection && (
          <SelectionPanel
            title={state.pendingSelection.title}
            items={state.pendingSelection.items}
            allowRetry={state.pendingSelection.allow_retry}
            onAnswer={handleSelectionAnswer}
          />
        )}

        {/* Text / choice input */}
        {state.pendingInput &&
          !state.pendingSelection &&
          (() => {
            const { prompt, options } = state.pendingInput
            if (options && options.length > 0) {
              return <ChoiceInputForm prompt={prompt} options={options} onSelect={handleAnswer} />
            }
            return <TextInputForm prompt={prompt} onSubmit={handleAnswer} />
          })()}
      </div>

      <ProgressBar plan={state.plan} completed={state.completed} activeNode={state.activeNode} />
    </div>
  )
}

function scrollToBottom() {
  setTimeout(() => {
    const container = document.querySelector('.chat-scroll')
    if (container) container.scrollTop = container.scrollHeight
  }, 50)
}
