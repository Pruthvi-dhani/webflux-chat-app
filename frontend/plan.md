# Plan: React Frontend for Reactive Chat App

Build a modern React frontend using **React 19**, **Redux Toolkit**, **TypeScript**, and **Vite** to consume the Spring WebFlux chat backend API and WebSocket endpoints.

---

## Tech Stack

- **React 19** + **TypeScript**
- **Vite** — build tooling
- **Redux Toolkit** + **RTK Query** — state management and API data fetching
- **React Router v7** — client-side routing
- **WebSocket (native)** — real-time chat messaging
- **Tailwind CSS v4** — styling
- **React Hook Form** + **Zod** — form handling and validation
- **Vitest** + **React Testing Library** — unit/integration tests

---

## Steps

### 1. Scaffold the project

- Initialize with `npm create vite@latest` using the React + TypeScript + SWC template.
- Install dependencies: `@reduxjs/toolkit`, `react-redux`, `react-router`, `tailwindcss`, `react-hook-form`, `zod`, `@hookform/resolvers`.
- Install dev dependencies: `vitest`, `@testing-library/react`, `@testing-library/jest-dom`, `jsdom`.
- Configure Vite proxy to forward `/api` and `/ws` requests to `http://localhost:8080` during development.
- Set up Tailwind CSS v4.
- Set up path aliases (`@/` → `src/`).

---

### 2. Set up Redux store and API slices

- **`store.ts`** — configure store with RTK Query middleware.
- **`authApi.ts`** (RTK Query) — endpoints:
  - `POST /api/auth/register` — `register({ username, password })`
  - `POST /api/auth/login` — `login({ username, password })` → returns JWT token
- **`authSlice.ts`** — holds `token`, `user` (id, username, role) in state; persist token to `localStorage`.
- **`chatApi.ts`** (RTK Query) — endpoints:
  - `GET /api/rooms` — `getRooms()`
  - `GET /api/rooms/{id}` — `getRoom(id)`
  - `POST /api/rooms` — `createRoom({ name })`
  - `POST /api/rooms/{id}/join` — `joinRoom(id)`
  - `GET /api/rooms/{id}/messages?page&size` — `getMessages({ roomId, page, size })`
- **`baseQuery`** — custom `fetchBaseQuery` that injects `Authorization: Bearer <token>` header from auth state.
- **Tests**:
  - `authSlice` — token storage, logout clears state, rehydration from `localStorage`.
  - `baseQuery` — injects auth header when token present, omits when absent.

---

### 3. Set up routing and layout

- **Routes**:
  - `/login` — login page
  - `/register` — registration page
  - `/` — redirect to `/rooms`
  - `/rooms` — chat rooms list
  - `/rooms/:id` — chat room view with messages
- **`ProtectedRoute`** component — redirects to `/login` if no token in state.
- **`AppLayout`** — shared layout with top nav bar (app name, username, logout button).

---

### 4. Build authentication pages

- **`LoginPage`**:
  - Form: username, password (validated with Zod)
  - On submit → call `login` mutation → store token + user in Redux → redirect to `/rooms`
  - Link to register page
- **`RegisterPage`**:
  - Form: username, password, confirm password (validated with Zod)
  - On submit → call `register` mutation → redirect to `/login` with success message
  - Link to login page
- Handle API errors (invalid credentials, username taken) and display inline.
- **Tests**:
  - `LoginPage` — renders form, shows validation errors, calls login mutation on submit, redirects on success.
  - `RegisterPage` — renders form, validates password match, calls register mutation, shows error on duplicate username.
  - Zod schemas — validate required fields, password length, confirm password match.
  - Mock API calls with MSW or RTK Query test utilities.

---

### 5. Build chat rooms list page

- **`RoomsPage`**:
  - Fetch rooms via `getRooms()` query on mount.
  - Display rooms as a list/grid with room name, creator, and created date.
  - "Create Room" button → opens a modal/dialog with a form (room name) → calls `createRoom` mutation → refetch list.
  - "Join" button on rooms the user hasn't joined yet → calls `joinRoom` mutation.
  - Click on a room → navigate to `/rooms/:id`.
- **Tests**:
  - `RoomsPage` — renders room list from mocked API, create room flow opens modal and calls mutation, join room button calls mutation and updates UI.

---

### 6. Build chat room view with message history

- **`ChatRoomPage`**:
  - Fetch room details via `getRoom(id)`.
  - Fetch initial message history via `getMessages({ roomId, page: 0, size: 50 })`.
  - Display messages in a scrollable container, oldest at top, newest at bottom.
  - Each message shows: sender username, content, timestamp.
  - System messages (JOIN/LEAVE type) styled differently from chat messages.
  - Infinite scroll upward to load older pages.
- **Tests**:
  - `ChatRoomPage` — renders messages from mocked API, system messages styled differently, infinite scroll triggers next page fetch.

---

### 7. Integrate WebSocket for real-time messaging

- **`useWebSocket` hook**:
  - Connects to `ws://host/ws/chat/{roomId}?token=<jwt>` on mount.
  - Receives incoming messages → appends to local message list (via Redux or component state).
  - Provides a `sendMessage(content)` function that sends JSON over the socket.
  - Handles reconnection on disconnect with exponential backoff.
  - Cleans up connection on unmount.
- **Message input bar** at the bottom of `ChatRoomPage`:
  - Text input + send button (and Enter key to send).
  - Disabled while WebSocket is not connected.
- **Tests**:
  - `useWebSocket` hook — connection lifecycle, sends message, receives message, reconnects on disconnect.
  - Message input bar — sends on Enter key, sends on button click, disabled when disconnected.
