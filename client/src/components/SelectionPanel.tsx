import { useState } from 'react'
import type { SelectionItem } from '../protocol/types'

interface Props {
  title: string
  items: SelectionItem[]
  allowRetry: boolean
  onAnswer: (id: string) => void
}

export function SelectionPanel({ title, items, allowRetry, onAnswer }: Props) {
  return (
    <div className="border-t border-gray-200 bg-white">
      {/* Header */}
      <div className="flex items-center justify-between border-b border-gray-100 px-5 py-3">
        <div>
          <span className="text-xs font-semibold tracking-wider text-indigo-600 uppercase">
            {title}
          </span>
          <span className="ml-3 text-xs text-gray-400">
            {items.length} option{items.length === 1 ? '' : 's'} · ranked by evidence score
          </span>
        </div>
        {allowRetry && (
          <button
            className="rounded-lg border border-gray-200 px-3 py-1.5 text-xs font-medium text-gray-500 transition-colors hover:border-red-300 hover:bg-red-50 hover:text-red-600"
            onClick={() => onAnswer('retry')}
          >
            None of these
          </button>
        )}
      </div>

      {/* Items */}
      <div className="overflow-y-auto" style={{ maxHeight: '420px' }}>
        {items.map((item, idx) => (
          <SelectionItemRow key={item.id} item={item} idx={idx} onAnswer={onAnswer} />
        ))}
      </div>
    </div>
  )
}

interface ItemProps {
  item: SelectionItem
  idx: number
  onAnswer: (id: string) => void
}

function SelectionItemRow({ item, idx, onAnswer }: ItemProps) {
  const [expanded, setExpanded] = useState(false)

  return (
    <div className="border-b border-gray-50 last:border-0">
      <div className="flex items-start gap-3 px-5 py-4 transition-colors hover:bg-gray-50">
        {/* Rank + score */}
        <div className="flex flex-shrink-0 flex-col items-center gap-1.5 pt-0.5">
          <span className="text-xs font-bold text-gray-400 tabular-nums">#{idx + 1}</span>
          {item.score !== null && <ScoreBadge score={item.score} />}
        </div>

        {/* Content */}
        <div className="min-w-0 flex-1">
          <p className="text-sm leading-snug font-medium text-gray-800">{item.label}</p>
          {item.description && (
            <p className="mt-0.5 text-xs leading-relaxed text-gray-500">{item.description}</p>
          )}
          {item.tags.length > 0 && (
            <div className="mt-2 flex flex-wrap gap-1.5">
              {item.tags.map((tag) => (
                <span
                  key={tag}
                  className="rounded-md bg-gray-100 px-2 py-0.5 text-xs font-medium text-gray-600"
                >
                  {tag}
                </span>
              ))}
            </div>
          )}
          {item.details.length > 0 && (
            <div>
              <button
                className="mt-2 flex items-center gap-1 text-xs font-medium text-indigo-500 transition-colors hover:text-indigo-700"
                onClick={() => setExpanded((e) => !e)}
              >
                {expanded ? '▾ Hide details' : '▸ Show details'}
              </button>
              {expanded && (
                <div className="mt-2 space-y-1">
                  {item.details.map((d) => (
                    <div key={d.label} className="flex gap-2 text-xs">
                      <span className="w-20 flex-shrink-0 font-medium text-gray-500">
                        {d.label}:
                      </span>
                      <span className="leading-relaxed text-gray-700">{d.value}</span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>

        {/* Select button */}
        <div className="flex-shrink-0 pl-2">
          <button
            className="rounded-lg bg-indigo-600 px-4 py-2 text-xs font-semibold whitespace-nowrap text-white shadow-sm transition-colors hover:bg-indigo-700 active:bg-indigo-800"
            onClick={() => onAnswer(item.id)}
          >
            Select
          </button>
        </div>
      </div>
    </div>
  )
}

function ScoreBadge({ score }: { score: number }) {
  const cls =
    score >= 7.0
      ? 'bg-green-100 text-green-700'
      : score >= 4.0
        ? 'bg-amber-100 text-amber-700'
        : 'bg-red-100 text-red-600'

  return (
    <span className={`rounded px-1.5 py-0.5 text-xs font-bold tabular-nums ${cls}`}>
      {score.toFixed(1)}
    </span>
  )
}
