// Error classes using plain properties (no parameter properties — required by erasableSyntaxOnly).

export class NetworkError {
  readonly _tag = 'NetworkError' as const
  readonly message: string
  constructor(message: string) {
    this.message = message
  }
}

export class ApiError {
  readonly _tag = 'ApiError' as const
  readonly message: string
  readonly status: number
  constructor(message: string, status: number) {
    this.message = message
    this.status = status
  }
}

export class UnauthorizedError {
  readonly _tag = 'UnauthorizedError' as const
  readonly message = 'Unauthorized'
}

export class SseConnectionError {
  readonly _tag = 'SseConnectionError' as const
  readonly message = 'SSE connection error'
}

export class SseParseError {
  readonly _tag = 'SseParseError' as const
  readonly message: string
  readonly data: string
  constructor(data: string) {
    this.data = data
    this.message = `Failed to parse SSE event: ${data}`
  }
}

export type AppError =
  | NetworkError
  | ApiError
  | UnauthorizedError
  | SseConnectionError
  | SseParseError

export function errorMessage(err: AppError): string {
  return err.message ?? 'Unknown error'
}
