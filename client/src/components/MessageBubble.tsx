import type { WorkflowStep } from '../protocol/types'
import { WORKFLOW_STEP_NAMES } from '../protocol/types'

interface AgentProps {
  content: string
  sourceNode: WorkflowStep | null
}

export function AgentMessage({ content, sourceNode }: AgentProps) {
  return (
    <div className="mb-4 flex justify-start">
      <div className="max-w-2xl rounded-2xl rounded-tl-sm border border-gray-100 bg-white px-5 py-3 shadow-sm">
        {sourceNode && (
          <span className="mb-1 block text-xs font-medium text-indigo-500">
            {WORKFLOW_STEP_NAMES[sourceNode]}
          </span>
        )}
        <p className="text-sm leading-relaxed whitespace-pre-wrap text-gray-800">{content}</p>
      </div>
    </div>
  )
}

export function UserMessage({ content }: { content: string }) {
  return (
    <div className="mb-4 flex justify-end">
      <div className="max-w-2xl rounded-2xl rounded-tr-sm bg-indigo-600 px-5 py-3 shadow-sm">
        <p className="text-sm leading-relaxed whitespace-pre-wrap text-white">{content}</p>
      </div>
    </div>
  )
}

export function StateMessage({ content }: { content: string }) {
  return (
    <div className="mb-3 flex justify-center">
      <div className="rounded-full bg-gray-100 px-4 py-2 text-xs font-medium text-gray-500">
        {content}
      </div>
    </div>
  )
}

interface ErrorMessageProps {
  message: string
  onRetry: () => void
}

export function ErrorMessage({ message, onRetry }: ErrorMessageProps) {
  return (
    <div className="mb-4 flex justify-center">
      <div className="flex max-w-lg items-start gap-3 rounded-xl border border-red-200 bg-red-50 px-5 py-3">
        <span className="mt-0.5 flex-shrink-0 text-base text-red-500">⚠</span>
        <div className="flex-1">
          <p className="text-sm leading-snug font-medium text-red-700">Something went wrong</p>
          <p className="mt-1 text-xs leading-relaxed text-red-500">{message}</p>
          <button
            className="mt-3 rounded-lg bg-red-500 px-3 py-1.5 text-xs font-medium text-white transition-colors hover:bg-red-600"
            onClick={onRetry}
          >
            Retry
          </button>
        </div>
      </div>
    </div>
  )
}
