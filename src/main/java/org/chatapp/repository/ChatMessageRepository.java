package org.chatapp.repository;
import org.chatapp.model.ChatMessage;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
public interface ChatMessageRepository extends ReactiveCrudRepository<ChatMessage, Long> {
    @Query("SELECT * FROM chat_messages WHERE room_id = :roomId ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    Flux<ChatMessage> findByRoomIdPaginated(Long roomId, int limit, int offset);
    @Query("SELECT * FROM chat_messages WHERE room_id = :roomId ORDER BY created_at ASC")
    Flux<ChatMessage> findByRoomIdOrderByCreatedAtAsc(Long roomId);
}
