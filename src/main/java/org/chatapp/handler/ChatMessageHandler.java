package org.chatapp.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chatapp.dto.ChatMessageResponse;
import org.chatapp.repository.ChatMessageRepository;
import org.chatapp.repository.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageHandler {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    /**
     * GET /api/rooms/{id}/messages?page=0&size=50 — paginated message history with sender username.
     */
    public Mono<ServerResponse> getMessages(ServerRequest request) {
        Long roomId = Long.parseLong(request.pathVariable("id"));
        int page = Integer.parseInt(request.queryParam("page").orElse("0"));
        int size = Integer.parseInt(request.queryParam("size").orElse("50"));
        int offset = page * size;

        return chatMessageRepository.findByRoomIdPaginated(roomId, size, offset)
                .flatMap(message -> userRepository.findById(message.getSenderId())
                        .map(user -> ChatMessageResponse.builder()
                                .id(message.getId())
                                .roomId(message.getRoomId())
                                .senderId(message.getSenderId())
                                .senderUsername(user.getUsername())
                                .content(message.getContent())
                                .type(message.getType().name())
                                .createdAt(message.getCreatedAt())
                                .build()
                        )
                        .defaultIfEmpty(ChatMessageResponse.builder()
                                .id(message.getId())
                                .roomId(message.getRoomId())
                                .senderId(message.getSenderId())
                                .senderUsername("unknown")
                                .content(message.getContent())
                                .type(message.getType().name())
                                .createdAt(message.getCreatedAt())
                                .build()
                        )
                )
                .collectList()
                .flatMap(messages -> ServerResponse.ok().bodyValue(messages));
    }
}

