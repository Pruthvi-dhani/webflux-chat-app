package org.chatapp.controller;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chatapp.dto.ChatMessageResponse;
import org.chatapp.repository.ChatMessageRepository;
import org.chatapp.repository.UserRepository;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import java.util.List;
@Slf4j
@RestController
@RequestMapping("/api/rooms/{roomId}/messages")
@RequiredArgsConstructor
public class ChatMessageController {
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    @GetMapping
    public Mono<List<ChatMessageResponse>> getMessages(
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
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
                .collectList();
    }
}
