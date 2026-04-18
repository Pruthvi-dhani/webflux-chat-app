package org.chatapp.service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chatapp.dto.ChatMessageResponse;
import org.chatapp.repository.ChatMessageRepository;
import org.chatapp.repository.UserRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    public Flux<ChatMessageResponse> getMessagesByRoomId(Long roomId, int page, int size) {
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
                );
    }
}
