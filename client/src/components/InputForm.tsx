import { useState } from 'react'

interface TextInputProps {
  prompt: string
  onSubmit: (value: string) => void
}

export function TextInputForm({ prompt, onSubmit }: TextInputProps) {
  const [value, setValue] = useState('')

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (value.trim()) {
      onSubmit(value)
      setValue('')
    }
  }

  return (
    <div className="border-t border-gray-200 bg-white p-4">
      <p className="mb-3 text-sm font-medium text-gray-600">{prompt}</p>
      <form className="flex gap-3" onSubmit={handleSubmit}>
        <input
          className="flex-1 rounded-xl border border-gray-300 px-4 py-3 text-sm focus:border-transparent focus:ring-2 focus:ring-indigo-500 focus:outline-none"
          placeholder="Type your answer..."
          value={value}
          onChange={(e) => setValue(e.target.value)}
          autoFocus
        />
        <button
          type="submit"
          className="rounded-xl bg-indigo-600 px-6 py-3 text-sm font-medium text-white shadow-sm transition-colors hover:bg-indigo-700"
        >
          Send
        </button>
      </form>
    </div>
  )
}

interface ChoiceInputProps {
  prompt: string
  options: string[]
  onSelect: (value: string) => void
}

export function ChoiceInputForm({ prompt, options, onSelect }: ChoiceInputProps) {
  return (
    <div className="border-t border-gray-200 bg-white p-4">
      <p className="mb-3 text-sm font-medium text-gray-600">{prompt}</p>
      <div className="flex flex-wrap gap-2">
        {options.map((opt) => (
          <button
            key={opt}
            className="rounded-xl border-2 border-gray-200 bg-white px-5 py-2.5 text-sm font-medium text-gray-700 transition-all hover:border-indigo-500 hover:bg-indigo-50"
            onClick={() => onSelect(opt)}
          >
            {opt}
          </button>
        ))}
      </div>
    </div>
  )
}
