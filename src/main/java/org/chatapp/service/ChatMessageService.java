package org.chatapp.service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chatapp.dto.ChatMessageResponse;
import org.chatapp.repository.ChatMessageRepository;
import org.chatapp.repository.ChatRoomMemberRepository;
import org.chatapp.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final UserRepository userRepository;

    public Flux<ChatMessageResponse> getMessagesByRoomId(Long roomId, Long userId, int page, int size) {
        int offset = page * size;
        return chatRoomMemberRepository.existsByRoomIdAndUserId(roomId, userId)
                .flatMapMany(isMember -> {
                    if (!isMember) {
                        return Flux.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this room"));
                    }
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
                });
    }
}
