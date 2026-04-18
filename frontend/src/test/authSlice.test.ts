import { describe, it, expect, beforeEach, vi } from 'vitest'
import authReducer, { setCredentials, logout } from '../store/authSlice'
import type { AuthState } from '../store/authSlice'

const mockLocalStorage = (() => {
  let store: Record<string, string> = {}
  return {
    getItem: vi.fn((key: string) => store[key] ?? null),
    setItem: vi.fn((key: string, value: string) => {
      store[key] = value
    }),
    removeItem: vi.fn((key: string) => {
      delete store[key]
    }),
    clear: vi.fn(() => {
      store = {}
    }),
  }
})()

Object.defineProperty(globalThis, 'localStorage', { value: mockLocalStorage })

describe('authSlice', () => {
  beforeEach(() => {
    mockLocalStorage.clear()
    vi.clearAllMocks()
  })

  it('should have null initial state when localStorage is empty', () => {
    const state = authReducer(undefined, { type: 'unknown' })
    expect(state.token).toBeNull()
    expect(state.user).toBeNull()
  })

  it('should store credentials on setCredentials', () => {
    const payload = { token: 'jwt-token-123', userId: 1, username: 'alice' }
    const state = authReducer(undefined, setCredentials(payload))

    expect(state.token).toBe('jwt-token-123')
    expect(state.user).toEqual({ userId: 1, username: 'alice' })
    expect(mockLocalStorage.setItem).toHaveBeenCalledWith('token', 'jwt-token-123')
    expect(mockLocalStorage.setItem).toHaveBeenCalledWith(
      'user',
      JSON.stringify({ userId: 1, username: 'alice' }),
    )
  })

  it('should clear state and localStorage on logout', () => {
    const loggedIn: AuthState = {
      token: 'jwt-token-123',
      user: { userId: 1, username: 'alice' },
    }
    const state = authReducer(loggedIn, logout())

    expect(state.token).toBeNull()
    expect(state.user).toBeNull()
    expect(mockLocalStorage.removeItem).toHaveBeenCalledWith('token')
    expect(mockLocalStorage.removeItem).toHaveBeenCalledWith('user')
  })

  it('should overwrite credentials when setCredentials is called again', () => {
    const first = { token: 'token-1', userId: 1, username: 'alice' }
    const second = { token: 'token-2', userId: 2, username: 'bob' }

    let state = authReducer(undefined, setCredentials(first))
    state = authReducer(state, setCredentials(second))

    expect(state.token).toBe('token-2')
    expect(state.user).toEqual({ userId: 2, username: 'bob' })
  })
})
