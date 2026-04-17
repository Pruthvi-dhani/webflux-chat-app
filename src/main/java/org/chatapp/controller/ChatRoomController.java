package org.chatapp.controller;

import lombok.RequiredArgsConstructor;
import org.chatapp.dto.ChatRoomResponse;
import org.chatapp.dto.CreateRoomRequest;
import org.chatapp.service.ChatRoomService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ChatRoomResponse> createRoom(@RequestBody CreateRoomRequest request) {
        return extractUserId()
                .flatMap(userId -> chatRoomService.createRoom(request.getName(), userId));
    }

    @GetMapping
    public Mono<List<ChatRoomResponse>> listRooms() {
        return extractUserId()
                .flatMapMany(chatRoomService::listRoomsForUser)
                .collectList();
    }

    @GetMapping("/{id}")
    public Mono<ChatRoomResponse> getRoom(@PathVariable Long id) {
        return chatRoomService.getRoomById(id);
    }

    @PostMapping("/{id}/join")
    public Mono<String> joinRoom(@PathVariable Long id) {
        return extractUserId()
                .flatMap(userId -> chatRoomService.joinRoom(id, userId))
                .thenReturn("Joined room successfully");
    }

    private Mono<Long> extractUserId() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .cast(UsernamePasswordAuthenticationToken.class)
                .map(auth -> (Long) auth.getDetails());
    }
}
