import { useState, useRef, useCallback, useEffect } from 'react'
import { useParams } from '@tanstack/react-router'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import type { SseEvent, SseInputRequest, SseSelectionRequest, WorkflowStep } from '../protocol/types'
import { SSE_EVENT_TYPES } from '../protocol/types'
import { parseSseEvent } from '../protocol/parse'
import { getHistoryApi, submitInputApi } from '../api/chat'
import { errorMessage } from '../api/errors'
import { ConversationSidebar } from '../components/ConversationSidebar'
import { AgentMessage, ErrorMessage, StateMessage, UserMessage } from '../components/MessageBubble'
import { ProgressBar } from '../components/ProgressBar'
import { NodeCompleteCard, WorkflowCompleteCard } from '../components/ResultCard'
import { ChoiceInputForm, TextInputForm } from '../components/InputForm'
import { SelectionPanel } from '../components/SelectionPanel'

// ─── Types ────────────────────────────────────────────────────────────────────

type ChatItem =
  | { kind: 'agent'; content: string; node: WorkflowStep | null }
  | { kind: 'user'; content: string }
  | { kind: 'state'; content: string }
  | { kind: 'nodeDone'; step: WorkflowStep }
  | { kind: 'done'; chatId: string }
  | { kind: 'error'; message: string }

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

export function ChatPage() {
  const { chatId } = useParams({ from: '/_app/chat/$chatId' })
  const queryClient = useQueryClient()
  const [state, setState] = useState<ChatState>(INITIAL_STATE)
  const [retryCount, setRetryCount] = useState(0)

  const processEventRef = useRef<(event: SseEvent) => void>(() => undefined)

  processEventRef.current = useCallback(
    (event: SseEvent) => {
      switch (event.type) {
        case 'workflow_plan':
          setState((prev) => ({ ...prev, plan: event.steps }))
          break
        case 'message':
          setState((prev) => ({
            ...prev,
            items: [...prev.items, { kind: 'agent', content: event.content, node: event.source_node }],
            reportContent:
              event.source_node === 'markdown_generation'
                ? (prev.reportContent ?? '') + '\n' + event.content
                : prev.reportContent,
          }))
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
    []
  )

  // Load history then open SSE stream. Cleans up on chatId / retry change.
  useEffect(() => {
    setState(INITIAL_STATE)
    let es: EventSource | null = null
    let cancelled = false

    const run = async () => {
      let liveIndex = 0
      try {
        const history = await getHistoryApi(chatId)
        if (cancelled) return
        history.events.forEach((ev) => processEventRef.current(ev))
        liveIndex = history.liveIndex
        if (history.events.some((ev) => ev.type === 'workflow_complete')) return
      } catch (err) {
        if (!cancelled)
          setState((prev) => ({
            ...prev,
            items: [{ kind: 'error', message: `Failed to load conversation: ${errorMessage(err)}` }],
          }))
        return
      }

      const url = `/api/chat/${chatId}/stream?after=${liveIndex}`
      es = new EventSource(url)

      SSE_EVENT_TYPES.forEach((evtType) => {
        es!.addEventListener(evtType, (e: Event) => {
          const msg = e as MessageEvent<string>
          try {
            processEventRef.current(parseSseEvent(msg.data))
          } catch {
            // ignore parse errors
          }
        })
      })

      es.onerror = () => {
        if (!cancelled)
          setState((prev) => ({
            ...prev,
            items: [...prev.items, { kind: 'error', message: 'Connection lost' }],
          }))
        es?.close()
      }
    }

    void run()
    return () => {
      cancelled = true
      es?.close()
    }
  }, [chatId, retryCount])

  const handleRetry = useCallback(() => setRetryCount((c) => c + 1), [])

  const submitInput = useMutation({
    mutationFn: ({ input }: { input: string; savedPending: SseInputRequest | null }) =>
      submitInputApi(chatId, input),
    onError: (_err, { savedPending }) => {
      toast.error('Failed to send your message')
      setState((prev) => ({ ...prev, pendingInput: savedPending }))
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['chats'] })
    },
  })

  const handleAnswer = useCallback(
    (answer: string) => {
      const savedPending = state.pendingInput
      setState((prev) => ({
        ...prev,
        pendingInput: null,
        items: [...prev.items, { kind: 'user', content: answer }],
      }))
      submitInput.mutate({ input: answer, savedPending })
    },
    [state.pendingInput, submitInput]
  )

  const handleSelectionAnswer = useCallback(
    (id: string) => {
      setState((prev) => ({ ...prev, pendingSelection: null }))
      submitInput.mutate({ input: id, savedPending: null })
    },
    [submitInput]
  )

  return (
    <div className="flex bg-gray-50" style={{ height: 'calc(100vh - 3.5rem)' }}>
      <ConversationSidebar activeChatId={chatId} />

      <div className="flex min-w-0 flex-1 flex-col">
        <div className="flex shrink-0 items-center justify-between border-b border-gray-200 bg-white px-6 py-4">
          <h2 className="text-lg font-semibold text-gray-800">Brainstorming Session</h2>
          <span className="font-mono text-xs text-gray-400">{chatId.slice(0, 8)}</span>
        </div>

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

        {state.pendingSelection && (
          <SelectionPanel
            title={state.pendingSelection.title}
            items={state.pendingSelection.items}
            allowRetry={state.pendingSelection.allow_retry}
            onAnswer={handleSelectionAnswer}
          />
        )}

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
