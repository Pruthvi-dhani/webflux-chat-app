# Reactive Chat Backend

A reactive chat API built with **Spring Boot WebFlux**, **R2DBC + PostgreSQL**, **JWT authentication**, and **WebSocket** real-time messaging.

## Tech Stack

| Layer          | Technology                                      |
|----------------|--------------------------------------------------|
| Framework      | Spring Boot 3.5, Spring WebFlux                  |
| Database       | PostgreSQL via R2DBC (non-blocking)              |
| Migrations     | Flyway (JDBC)                                    |
| Auth           | Spring Security Reactive + JWT (JJWT 0.12.x)    |
| Real-time      | WebSocket (Reactor Sinks)                        |
| Build          | Gradle (Kotlin DSL), Java 25                     |
| Test           | JUnit 5, WebTestClient, H2 in-memory             |

## Prerequisites

- **Java 25**
- **PostgreSQL** running locally (or update connection config)
- **Gradle 9.2+** (wrapper included)

## Configuration

Set the following environment variables (or update `application.yml`):

| Variable                | Description                       | Default     |
|-------------------------|-----------------------------------|-------------|
| `DB_HOST`               | PostgreSQL host                   | `localhost` |
| `DB_PORT`               | PostgreSQL port                   | `5432`      |
| `DB_NAME`               | Database name                     | `chatdb`    |
| `DB_USERNAME`            | Database username                 | `postgres`  |
| `DB_PASSWORD`            | Database password                 | `postgres`  |
| `JWT_SECRET`             | JWT signing key (≥32 chars)       | —           |
| `JWT_EXPIRATION_SECONDS` | Token expiry in seconds           | —           |

## Running

```bash
# Create the database (if it doesn't exist)
createdb chatdb

# Run the application (Flyway auto-migrates on startup)
./gradlew bootRun
```

The server starts on **port 8080**.

## API Endpoints

### Authentication (public)

| Method | Endpoint             | Body                                    | Description         |
|--------|----------------------|-----------------------------------------|---------------------|
| POST   | `/api/auth/register` | `{"username": "...", "password": "..."}` | Register a new user |
| POST   | `/api/auth/login`    | `{"username": "...", "password": "..."}` | Login, returns JWT  |

**Response:**
```json
{
  "token": "eyJhbGciOi...",
  "username": "alice",
  "userId": 1
}
```

### Chat Rooms (JWT required)

All endpoints require `Authorization: Bearer <token>` header.

| Method | Endpoint                               | Body                  | Description                   |
|--------|----------------------------------------|-----------------------|-------------------------------|
| POST   | `/api/rooms`                           | `{"name": "General"}` | Create a room (auto-join)     |
| GET    | `/api/rooms`                           | —                     | List rooms you're a member of |
| GET    | `/api/rooms/{id}`                      | —                     | Get room details              |
| POST   | `/api/rooms/{id}/join`                 | —                     | Join a room                   |
| GET    | `/api/rooms/{id}/messages?page=0&size=50` | —                 | Paginated message history     |

### WebSocket (planned)

```
ws://localhost:8080/ws/chat/{roomId}?token=<jwt>
```

Send/receive JSON messages in real time.

## Database Schema

Managed via Flyway migrations in `src/main/resources/db/migration/`:

- `V1` — `users` table
- `V2` — `chat_rooms` table
- `V3` — `chat_room_members` join table
- `V4` — `chat_messages` table

## Project Structure

```
org.chatapp
├── ChatApplication.java          — Entry point
├── config/
│   ├── SecurityConfig.java       — WebFlux security, JWT filter registration
│   ├── R2dbcConfig.java          — R2DBC auditing
│   └── RouterConfig.java         — Functional route definitions
├── security/
│   ├── JwtUtil.java              — JWT token generation & validation
│   ├── JwtAuthenticationFilter.java — WebFilter extracting JWT from requests
│   └── CustomUserDetailsService.java — Loads users for Spring Security
├── model/                        — R2DBC entities (User, ChatRoom, ChatRoomMember, ChatMessage)
├── repository/                   — ReactiveCrudRepository interfaces
├── dto/                          — Request/response DTOs
├── controller/
│   └── AuthController.java       — Registration & login endpoints
├── handler/
│   ├── ChatRoomHandler.java      — Room CRUD handler functions
│   └── ChatMessageHandler.java   — Message history handler
├── filter/
│   └── RequestResponseLoggingFilter.java — Request/response logging with sensitive data masking
└── websocket/                    — (planned) WebSocket handler & config
```

## Request/Response Logging

All HTTP requests and responses are logged with:
- Correlation ID for traceability
- **Sensitive data masking**: `password`, `token`, `refreshToken` fields in JSON bodies and `Authorization`/`Cookie` headers are replaced with `*****`
- WebSocket upgrades and actuator endpoints are skipped

## Testing

```bash
# Run all tests
./gradlew test

# Run a specific test class
./gradlew test --tests "org.chatapp.handler.ChatRoomHandlerTest"
```

Tests use **H2 in-memory** database with the `test` profile (`application-test.yml`).

## License

MIT

