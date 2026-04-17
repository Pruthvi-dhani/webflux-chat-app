package org.chatapp.controller;

import lombok.RequiredArgsConstructor;
import org.chatapp.dto.ChatMessageResponse;
import org.chatapp.service.ChatMessageService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/rooms/{roomId}/messages")
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    @GetMapping
    public Mono<List<ChatMessageResponse>> getMessages(
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return chatMessageService.getMessagesByRoomId(roomId, page, size)
                .collectList();
    }
}
