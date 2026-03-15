import type { WorkflowStep } from '../protocol/types'
import { WORKFLOW_STEP_NAMES } from '../protocol/types'

interface NodeCompleteProps {
  step: WorkflowStep
}

export function NodeCompleteCard({ step }: NodeCompleteProps) {
  return (
    <div className="mb-2 flex justify-center">
      <div className="rounded-full border border-green-100 bg-green-50 px-3 py-1.5 text-xs font-medium text-green-600">
        ✓ {WORKFLOW_STEP_NAMES[step]} completed
      </div>
    </div>
  )
}

interface WorkflowCompleteProps {
  reportContent: string | null
}

export function WorkflowCompleteCard({ reportContent }: WorkflowCompleteProps) {
  return (
    <div className="my-6">
      <div className="flex justify-center">
        <div className="max-w-md rounded-2xl border border-green-200 bg-gradient-to-br from-green-50 to-emerald-50 p-8 text-center shadow-sm">
          <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-green-100">
            <span className="text-xl text-green-600">✓</span>
          </div>
          <h3 className="mb-2 text-lg font-semibold text-green-800">Brainstorming Complete!</h3>
          <p className="text-sm text-green-600">
            Your project analysis and report have been generated.
          </p>
        </div>
      </div>
      {reportContent && <Report markdown={reportContent} />}
    </div>
  )
}

function Report({ markdown }: { markdown: string }) {
  const copyToClipboard = () => {
    void navigator.clipboard.writeText(markdown)
  }

  return (
    <div className="mx-auto mt-6 max-w-3xl">
      <div className="overflow-hidden rounded-2xl border border-gray-200 bg-white shadow-sm">
        <div className="flex items-center justify-between border-b border-gray-200 bg-gray-50 px-6 py-4">
          <h3 className="text-sm font-semibold text-gray-800">Generated Report</h3>
          <button
            className="rounded-lg bg-indigo-600 px-3 py-1.5 text-xs font-medium text-white transition-colors hover:bg-indigo-700"
            onClick={copyToClipboard}
          >
            Copy Markdown
          </button>
        </div>
        <div className="prose prose-sm max-w-none px-6 py-6">
          {markdown.split('\n').map((line, i) => {
            if (line.startsWith('# '))
              return (
                <h2 key={i} className="mt-6 mb-3 text-xl font-bold text-gray-900">
                  {line.slice(2)}
                </h2>
              )
            if (line.startsWith('## '))
              return (
                <h3 key={i} className="mt-5 mb-2 text-lg font-semibold text-gray-800">
                  {line.slice(3)}
                </h3>
              )
            if (line.startsWith('### '))
              return (
                <h4 key={i} className="mt-4 mb-1 text-base font-medium text-gray-700">
                  {line.slice(4)}
                </h4>
              )
            if (line.startsWith('- '))
              return (
                <div key={i} className="my-1 ml-4 flex gap-2">
                  <span className="text-gray-400">•</span>
                  <span className="text-sm text-gray-700">{line.slice(2)}</span>
                </div>
              )
            if (line.trim() === '') return <div key={i} className="h-3" />
            return (
              <p key={i} className="my-1 text-sm leading-relaxed text-gray-700">
                {line}
              </p>
            )
          })}
        </div>
      </div>
    </div>
  )
}
