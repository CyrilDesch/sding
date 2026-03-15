export class NetworkError extends Error {
  readonly _tag = 'NetworkError' as const
  constructor(message: string) {
    super(message)
    this.name = 'NetworkError'
  }
}

export class ApiError extends Error {
  readonly _tag = 'ApiError' as const
  readonly status: number
  constructor(message: string, status: number) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

export class UnauthorizedError extends Error {
  readonly _tag = 'UnauthorizedError' as const
  constructor() {
    super('Unauthorized')
    this.name = 'UnauthorizedError'
  }
}

export function errorMessage(err: unknown): string {
  if (err instanceof Error) return err.message
  return 'Unknown error'
}
