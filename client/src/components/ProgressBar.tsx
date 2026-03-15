import type { WorkflowStep } from '../protocol/types'
import { WORKFLOW_STEP_NAMES } from '../protocol/types'

interface Props {
  plan: WorkflowStep[]
  completed: Set<WorkflowStep>
  activeNode: WorkflowStep | null
}

export function ProgressBar({ plan, completed, activeNode }: Props) {
  return (
    <nav className="w-64 flex-shrink-0 overflow-y-auto border-l border-gray-200 bg-white px-4 py-6">
      <h3 className="mb-4 px-2 text-xs font-semibold tracking-wider text-gray-400 uppercase">
        Workflow Steps
      </h3>
      <ul className="space-y-1">
        {plan.map((step) => {
          const isDone = completed.has(step)
          const isActive = activeNode === step

          const itemCls = isDone
            ? 'flex items-center gap-3 px-3 py-2 rounded-lg text-sm transition-colors text-green-700 bg-green-50'
            : isActive
              ? 'flex items-center gap-3 px-3 py-2 rounded-lg text-sm transition-colors text-indigo-700 bg-indigo-50 font-medium'
              : 'flex items-center gap-3 px-3 py-2 rounded-lg text-sm transition-colors text-gray-400'

          const dotCls = isDone
            ? 'w-2 h-2 rounded-full flex-shrink-0 bg-green-500'
            : isActive
              ? 'w-2 h-2 rounded-full flex-shrink-0 bg-indigo-500 animate-pulse'
              : 'w-2 h-2 rounded-full flex-shrink-0 bg-gray-300'

          return (
            <li key={step} className={itemCls}>
              <span className={dotCls} />
              <span>{WORKFLOW_STEP_NAMES[step]}</span>
            </li>
          )
        })}
      </ul>
    </nav>
  )
}
