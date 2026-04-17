package org.chatapp.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chatapp.dto.ChatRoomResponse;
import org.chatapp.dto.CreateRoomRequest;
import org.chatapp.model.ChatRoom;
import org.chatapp.model.ChatRoomMember;
import org.chatapp.repository.ChatRoomMemberRepository;
import org.chatapp.repository.ChatRoomRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ChatRoomResponse> createRoom(@RequestBody CreateRoomRequest request) {
        return extractUserId()
                .flatMap(userId -> {
                    if (request.getName() == null || request.getName().isBlank()) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Room name is required"));
                    }
                    ChatRoom room = ChatRoom.builder()
                            .name(request.getName().trim())
                            .creatorId(userId)
                            .build();
                    return chatRoomRepository.save(room)
                            .flatMap(savedRoom -> {
                                ChatRoomMember member = ChatRoomMember.builder()
                                        .roomId(savedRoom.getId())
                                        .userId(userId)
                                        .build();
                                return chatRoomMemberRepository.save(member)
                                        .thenReturn(savedRoom);
                            });
                })
                .map(this::toResponse);
    }

    @GetMapping
    public Mono<List<ChatRoomResponse>> listRooms() {
        return extractUserId()
                .flatMapMany(userId -> chatRoomMemberRepository.findByUserId(userId)
                        .flatMap(member -> chatRoomRepository.findById(member.getRoomId()))
                )
                .map(this::toResponse)
                .collectList();
    }

    @GetMapping("/{id}")
    public Mono<ChatRoomResponse> getRoom(@PathVariable Long id) {
        return chatRoomRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found")))
                .map(this::toResponse);
    }

    @PostMapping("/{id}/join")
    public Mono<String> joinRoom(@PathVariable Long id) {
        return extractUserId()
                .flatMap(userId -> chatRoomRepository.findById(id)
                        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found")))
                        .then(chatRoomMemberRepository.existsByRoomIdAndUserId(id, userId))
                        .flatMap(exists -> {
                            if (exists) {
                                return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "Already a member of this room"));
                            }
                            ChatRoomMember member = ChatRoomMember.builder()
                                    .roomId(id)
                                    .userId(userId)
                                    .build();
                            return chatRoomMemberRepository.save(member);
                        })
                )
                .thenReturn("Joined room successfully");
    }

    private ChatRoomResponse toResponse(ChatRoom room) {
        return ChatRoomResponse.builder()
                .id(room.getId())
                .name(room.getName())
                .creatorId(room.getCreatorId())
                .createdAt(room.getCreatedAt())
                .build();
    }

    private Mono<Long> extractUserId() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .cast(UsernamePasswordAuthenticationToken.class)
                .map(auth -> (Long) auth.getDetails());
    }
}

