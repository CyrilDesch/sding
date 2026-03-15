import { useToastService } from '../hooks/useToastService'

export function Toast() {
  const { toast, dismiss } = useToastService()

  if (!toast) return null

  const bgCls =
    toast.variant === 'error'
      ? 'bg-red-600'
      : toast.variant === 'success'
        ? 'bg-green-600'
        : 'bg-gray-800'

  return (
    <div className="fixed bottom-6 left-1/2 z-50 -translate-x-1/2">
      <div
        className={`flex items-center gap-3 rounded-xl px-5 py-3 text-sm font-medium text-white shadow-lg ${bgCls}`}
      >
        <span>{toast.message}</span>
        <button
          className="ml-1 text-base leading-none text-white/70 hover:text-white"
          onClick={dismiss}
          aria-label="Dismiss"
        >
          ✕
        </button>
      </div>
    </div>
  )
}
