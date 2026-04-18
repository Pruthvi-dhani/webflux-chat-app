export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
}

export interface AuthResponse {
  token: string
  username: string
  userId: number
}

export interface CreateRoomRequest {
  name: string
}

export interface ChatRoomResponse {
  id: number
  name: string
  creatorId: number
  createdAt: string
}

export interface ChatMessageResponse {
  id: number
  roomId: number
  senderId: number
  senderUsername: string
  content: string
  type: 'CHAT' | 'JOIN' | 'LEAVE'
  createdAt: string
}
