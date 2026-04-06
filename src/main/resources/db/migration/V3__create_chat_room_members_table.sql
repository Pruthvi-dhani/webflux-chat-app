CREATE TABLE IF NOT EXISTS chat_room_members (
    id        BIGSERIAL PRIMARY KEY,
    room_id   BIGINT    NOT NULL REFERENCES chat_rooms(id) ON DELETE CASCADE,
    user_id   BIGINT    NOT NULL REFERENCES users(id)      ON DELETE CASCADE,
    joined_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_room_user UNIQUE (room_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_chat_room_members_room_id ON chat_room_members(room_id);
CREATE INDEX IF NOT EXISTS idx_chat_room_members_user_id ON chat_room_members(user_id);

