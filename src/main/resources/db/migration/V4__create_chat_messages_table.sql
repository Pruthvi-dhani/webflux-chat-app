CREATE TABLE IF NOT EXISTS chat_messages (
    id         BIGSERIAL PRIMARY KEY,
    room_id    BIGINT      NOT NULL REFERENCES chat_rooms(id) ON DELETE CASCADE,
    sender_id  BIGINT      NOT NULL REFERENCES users(id)      ON DELETE CASCADE,
    content    TEXT        NOT NULL,
    type       VARCHAR(10) NOT NULL DEFAULT 'CHAT',
    created_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_message_type CHECK (type IN ('CHAT', 'JOIN', 'LEAVE'))
);

CREATE INDEX IF NOT EXISTS idx_chat_messages_room_id     ON chat_messages(room_id);
CREATE INDEX IF NOT EXISTS idx_chat_messages_created_at  ON chat_messages(room_id, created_at DESC);

