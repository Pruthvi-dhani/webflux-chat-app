# Plan: Reactive Chat Backend with JWT Auth & PostgreSQL

Build a reactive chat API using **Spring Boot WebFlux**, **R2DBC + PostgreSQL** for persistence, **Spring Security Reactive with JWT** for auth, and **WebSockets** for real-time messaging. R2DBC replaces MongoDB as the reactive database driver, and schema is managed via a `schema.sql` init script (R2DBC doesn't auto-create tables like JPA).

---

## Steps

### 1. Bootstrap Spring Boot WebFlux + R2DBC in `build.gradle.kts`

- Add plugins: `org.springframework.boot` (3.4.x), `io.spring.dependency-management`.
- Add dependencies:
  - `spring-boot-starter-webflux`
  - `spring-boot-starter-data-r2dbc`
  - `r2dbc-postgresql` (io.r2dbc)
  - `spring-boot-starter-security`
  - `jjwt-api` / `jjwt-impl` / `jjwt-jackson` (io.jsonwebtoken, 0.12.x)
  - `lombok`
  - `spring-boot-starter-test`, `reactor-test`, `spring-security-test`
  - `io.r2dbc:r2dbc-h2` (for tests)
  - `org.flywaydb:flyway-core` and `org.flywaydb:flyway-database-postgresql` (Flyway for versioned schema migrations)
  - `org.postgresql:postgresql` (JDBC driver — required by Flyway since it uses JDBC, not R2DBC)
- Replace `Main.java` with a `@SpringBootApplication` class under `org.example.chatapp`.
- Create `application.yml` with R2DBC PostgreSQL URL (`r2dbc:postgresql://localhost:5432/chatdb`), JDBC URL for Flyway (`spring.flyway.url: jdbc:postgresql://localhost:5432/chatdb`), credentials, and JWT config (`app.jwt.secret`, `app.jwt.expiration-ms`).

---

### 2. Create database schema via Flyway migrations

Flyway runs migrations over JDBC before the R2DBC `ConnectionFactory` is used. Place versioned SQL scripts in `src/main/resources/db/migration/`.

Configure in `application.yml`:
```yaml
spring:
  flyway:
    enabled: true
    url: jdbc:postgresql://localhost:5432/chatdb
    user: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    locations: classpath:db/migration
```

**Migration: `V1__create_users_table.sql`**

**Table: `users`**

| Column       | Type                                  |
|--------------|---------------------------------------|
| `id`         | `BIGSERIAL PRIMARY KEY`               |
| `username`   | `VARCHAR(50) UNIQUE NOT NULL`         |
| `password`   | `VARCHAR(255) NOT NULL`               |
| `role`       | `VARCHAR(20) NOT NULL DEFAULT 'USER'` |
| `created_at` | `TIMESTAMP DEFAULT NOW()`             |
| `updated_at` | `TIMESTAMP DEFAULT NOW()`             |

**Migration: `V2__create_chat_rooms_table.sql`**

**Table: `chat_rooms`**

| Column       | Type                                 |
|--------------|--------------------------------------|
| `id`         | `BIGSERIAL PRIMARY KEY`              |
| `name`       | `VARCHAR(100) UNIQUE NOT NULL`       |
| `creator_id` | `BIGINT REFERENCES users(id)`        |
| `created_at` | `TIMESTAMP DEFAULT NOW()`            |
| `updated_at` | `TIMESTAMP DEFAULT NOW()`            |

**Migration: `V3__create_chat_room_members_table.sql`**

**Table: `chat_room_members`**

| Column      | Type                                |
|-------------|-------------------------------------|
| `id`        | `BIGSERIAL PRIMARY KEY`             |
| `room_id`   | `BIGINT REFERENCES chat_rooms(id)`  |
| `user_id`   | `BIGINT REFERENCES users(id)`       |
| `joined_at` | `TIMESTAMP DEFAULT NOW()`           |
| `updated_at`| `TIMESTAMP DEFAULT NOW()`           |
|             | `UNIQUE(room_id, user_id)`          |

**Migration: `V4__create_chat_messages_table.sql`**

**Table: `chat_messages`**

| Column       | Type                                                                     |
|--------------|--------------------------------------------------------------------------|
| `id`         | `BIGSERIAL PRIMARY KEY`                                                  |
| `room_id`    | `BIGINT REFERENCES chat_rooms(id)`                                       |
| `sender_id`  | `BIGINT REFERENCES users(id)`                                            |
| `content`    | `TEXT NOT NULL`                                                          |
| `type`       | `VARCHAR(10) NOT NULL DEFAULT 'CHAT'` — values: `CHAT`, `JOIN`, `LEAVE` |
| `created_at` | `TIMESTAMP DEFAULT NOW()`                                                |
| `updated_at` | `TIMESTAMP DEFAULT NOW()`                                                |

---

### 3. Create domain entities & reactive repositories (`model/`, `repository/`)

- Entity classes annotated with `@Table` and `@Id` (Spring Data R2DBC annotations — no JPA):
  - `User`, `ChatRoom`, `ChatRoomMember`, `ChatMessage`
- Repositories extending `ReactiveCrudRepository`:
  - `UserRepository` — add `findByUsername` returning `Mono<User>`
  - `ChatRoomRepository`
  - `ChatRoomMemberRepository` — add `findByRoomId`, `findByUserId`, `existsByRoomIdAndUserId`
  - `ChatMessageRepository` — add `findByRoomIdOrderByCreatedAtDesc` with pagination via custom `@Query` using `LIMIT`/`OFFSET`

---

### 4. Implement JWT authentication layer (`security/`, `dto/`, `controller/`)

- **`JwtUtil`** — wraps JJWT library:
  - `generateToken(username, userId)` — creates a signed JWT with claims
  - `validateToken(token)` — verifies signature and expiry
  - `extractClaims(token)` — returns username and userId
  - Reads secret/expiry from `@Value` config properties

- **`CustomUserDetailsService`** implementing `ReactiveUserDetailsService`:
  - Loads user from `UserRepository.findByUsername`

- **`JwtAuthenticationFilter`** (`WebFilter`):
  - Extracts `Authorization: Bearer <token>` header
  - Validates JWT via `JwtUtil`
  - Creates `UsernamePasswordAuthenticationToken` with user details
  - Sets it in `ReactiveSecurityContextHolder`

- **`SecurityConfig`** (`@EnableWebFluxSecurity`):
  - Stateless sessions, CSRF disabled
  - Permits `/api/auth/**` endpoints
  - Secures all other paths
  - Registers `JwtAuthenticationFilter` before `SecurityWebFiltersOrder.AUTHENTICATION`

- **`AuthController`** (`@RestController`, `/api/auth`):
  - `POST /register` — accepts `RegisterRequest` DTO → hash password with `BCryptPasswordEncoder` → save to DB → return success
  - `POST /login` — accepts `LoginRequest` DTO → verify credentials → return `AuthResponse` with JWT token

---

### 5. Add request/response logging with sensitive data masking (`filter/`)

- **`RequestResponseLoggingFilter`** (`WebFilter`, ordered before other filters):
  - **Request logging**: log HTTP method, URI, headers, and request body
  - **Response logging**: log status code, headers, and response body
  - **Sensitive data masking** — before logging, mask the following:
    - `password` fields in JSON bodies → replace value with `*****`
    - `token` fields in JSON bodies (e.g. in `AuthResponse`) → replace value with `*****`
    - `Authorization` header → log as `Bearer *****`
    - `Cookie` / `Set-Cookie` header values → replace with `*****`
  - Use a `ServerHttpRequestDecorator` to buffer and log the request body, then replay it
  - Use a `ServerHttpResponseDecorator` to intercept and log the response body before flushing
  - Masking logic in a utility method: regex-replace `"password"\s*:\s*"[^"]*"` → `"password":"*****"`, same pattern for `"token"`, `"refreshToken"`, etc.
  - Log at `INFO` level with a correlation/request ID (use `UUID` or extract from headers) for traceability
  - Skip logging for WebSocket upgrade requests and actuator endpoints
  - Register in `SecurityConfig` or as a `@Component` with `@Order(Ordered.HIGHEST_PRECEDENCE)`

---

### 6. Implement chat REST API with functional router + handler (`handler/`, `router/`)

- **`ChatRoomHandler`**:
  - `POST /api/rooms` — create room, set authenticated user as creator, auto-add to `chat_room_members`
  - `GET /api/rooms` — list all rooms for the authenticated user (join through `chat_room_members`)
  - `GET /api/rooms/{id}` — get room details
  - `POST /api/rooms/{id}/join` — add current user to `chat_room_members`

- **`ChatMessageHandler`**:
  - `GET /api/rooms/{id}/messages?page=0&size=50` — paginated message history with sender username (via custom `@Query` with `LIMIT`/`OFFSET`)

- **`RouterConfig`** class wiring all routes to handlers. All endpoints require a valid JWT.

---

### 7. Add real-time messaging via reactive WebSocket (`websocket/`)

- **`ChatWebSocketHandler`** implementing `WebSocketHandler`:
  - **On connect**: extract JWT from `?token=` query param (browsers can't send headers on WS upgrade), validate, extract `userId`/`username`, verify room membership via `ChatRoomMemberRepository`
  - **Room sinks**: maintain a `ConcurrentHashMap<Long, Sinks.Many<ChatMessage>>` keyed by `roomId`
  - **Inbound**: deserialize JSON → persist to `chat_messages` table → emit into room sink
  - **Outbound**: subscribe to room sink → serialize to JSON → send to client
  - **On disconnect**: broadcast a `LEAVE` message

- **`WebSocketConfig`**:
  - `SimpleUrlHandlerMapping` mapping `/ws/chat/{roomId}` to the handler
  - `WebSocketHandlerAdapter` bean

---

### 8. Add cross-cutting concerns & tests

- **`GlobalExceptionHandler`** (`@ControllerAdvice`):
  - Handle `ResponseStatusException`, auth errors, `DataIntegrityViolationException` (duplicate username/room name)
  - Return structured JSON error responses

- **`RequestLoggingFilter`** (`WebFilter`):
  - Log request method, path, status, and duration reactively

- **Tests** (using `WebTestClient` + R2DBC H2 in-memory):
  - `AuthControllerTest` — register, login, access protected route
  - `ChatRoomHandlerTest` — create room, join room, list rooms with JWT
  - `ChatWebSocketTest` — connect with valid/invalid token, send and receive messages
  - Test config: `application-test.yml` with `r2dbc:h2:mem:///testdb`

---

## Package Structure

```
org.example.chatapp
├── ChatApplication.java
├── config/          (SecurityConfig, WebSocketConfig, RouterConfig)
├── security/        (JwtUtil, JwtAuthenticationFilter, CustomUserDetailsService)
├── model/           (User, ChatRoom, ChatRoomMember, ChatMessage)
├── repository/      (UserRepository, ChatRoomRepository, ...)
├── dto/             (RegisterRequest, LoginRequest, AuthResponse, MessageDTO, ...)
├── controller/      (AuthController)
├── handler/         (ChatRoomHandler, ChatMessageHandler)
├── websocket/       (ChatWebSocketHandler)
└── exception/       (GlobalExceptionHandler)
```

---

## Further Considerations

1. **Room membership enforcement** — The plan includes a `chat_room_members` join table and membership checks on WebSocket connect + message history. Rooms could be made open-access instead for simplicity.
2. **R2DBC pagination** — R2DBC doesn't support `Pageable` as cleanly as JPA. Message pagination will use custom `@Query` with `LIMIT`/`OFFSET`.
3. **Schema migration** — Flyway is used for versioned migrations over JDBC. Future schema changes should be added as new versioned scripts (e.g. `V5__add_column.sql`) — never edit an already-applied migration.

