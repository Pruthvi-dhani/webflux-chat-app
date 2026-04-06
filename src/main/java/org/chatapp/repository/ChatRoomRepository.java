package org.chatapp.repository;
import org.chatapp.model.ChatRoom;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface ChatRoomRepository extends ReactiveCrudRepository<ChatRoom, Long> {
    Mono<Boolean> existsByName(String name);

}
