import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react'
import type { RootState } from './store'
import type { ChatRoomResponse, CreateRoomRequest, ChatMessageResponse } from '@/types/api'

const baseQuery = fetchBaseQuery({
  baseUrl: '/api',
  prepareHeaders: (headers, { getState }) => {
    const token = (getState() as RootState).auth.token
    if (token) {
      headers.set('Authorization', `Bearer ${token}`)
    }
    return headers
  },
})

export const chatApi = createApi({
  reducerPath: 'chatApi',
  baseQuery,
  tagTypes: ['Rooms'],
  endpoints: (builder) => ({
    getRooms: builder.query<ChatRoomResponse[], void>({
      query: () => '/rooms',
      providesTags: ['Rooms'],
    }),
    getRoom: builder.query<ChatRoomResponse, number>({
      query: (id) => `/rooms/${id}`,
    }),
    createRoom: builder.mutation<ChatRoomResponse, CreateRoomRequest>({
      query: (body) => ({
        url: '/rooms',
        method: 'POST',
        body,
      }),
      invalidatesTags: ['Rooms'],
    }),
    joinRoom: builder.mutation<string, number>({
      query: (id) => ({
        url: `/rooms/${id}/join`,
        method: 'POST',
      }),
      invalidatesTags: ['Rooms'],
    }),
    getMessages: builder.query<
      ChatMessageResponse[],
      { roomId: number; page?: number; size?: number }
    >({
      query: ({ roomId, page = 0, size = 50 }) =>
        `/rooms/${roomId}/messages?page=${page}&size=${size}`,
    }),
  }),
})

export const {
  useGetRoomsQuery,
  useGetRoomQuery,
  useCreateRoomMutation,
  useJoinRoomMutation,
  useGetMessagesQuery,
} = chatApi
