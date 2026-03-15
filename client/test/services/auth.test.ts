import { describe, it, expect, vi, beforeEach } from 'vitest'
import { checkAuthApi } from '../../src/api/auth'

describe('checkAuthApi', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('returns true when /api/auth/me responds ok', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true }))
    expect(await checkAuthApi()).toBe(true)
  })

  it('returns false when /api/auth/me responds with non-ok status', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: false, status: 401 }))
    expect(await checkAuthApi()).toBe(false)
  })

  it('returns false when fetch throws a network error', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('network error')))
    expect(await checkAuthApi()).toBe(false)
  })
})
