import { createSlice, type PayloadAction } from '@reduxjs/toolkit'

interface AuthUser {
  userId: number
  username: string
  role?: string
}

interface AuthState {
  token: string | null
  user: AuthUser | null
}

const tokenFromStorage = localStorage.getItem('token')
const userFromStorage = localStorage.getItem('user')

const initialState: AuthState = {
  token: tokenFromStorage,
  user: userFromStorage ? JSON.parse(userFromStorage) : null,
}

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    setCredentials(state, action: PayloadAction<{ token: string; userId: number; username: string }>) {
      const { token, userId, username } = action.payload
      state.token = token
      state.user = { userId, username }
      localStorage.setItem('token', token)
      localStorage.setItem('user', JSON.stringify({ userId, username }))
    },
    logout(state) {
      state.token = null
      state.user = null
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    },
  },
})

export const { setCredentials, logout } = authSlice.actions
export default authSlice.reducer
export type { AuthState, AuthUser }
