package org.chatapp.repository;
import org.chatapp.model.ChatRoomMember;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ChatRoomMemberRepository extends ReactiveCrudRepository<ChatRoomMember, Long> {

    Flux<ChatRoomMember> findByRoomId(Long roomId);

    Flux<ChatRoomMember> findByUserId(Long userId);

    Mono<Boolean> existsByRoomIdAndUserId(Long roomId, Long userId);

    Mono<Void> deleteByRoomIdAndUserId(Long roomId, Long userId);

}
