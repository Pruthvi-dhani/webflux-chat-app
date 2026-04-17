package org.chatapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chatapp.dto.ChatRoomResponse;
import org.chatapp.model.ChatRoom;
import org.chatapp.model.ChatRoomMember;
import org.chatapp.repository.ChatRoomMemberRepository;
import org.chatapp.repository.ChatRoomRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    public Mono<ChatRoomResponse> createRoom(String name, Long userId) {
        if (name == null || name.isBlank()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Room name is required"));
        }
        ChatRoom room = ChatRoom.builder()
                .name(name.trim())
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
                })
                .map(this::toResponse);
    }

    public Flux<ChatRoomResponse> listRoomsForUser(Long userId) {
        return chatRoomMemberRepository.findByUserId(userId)
                .flatMap(member -> chatRoomRepository.findById(member.getRoomId()))
                .map(this::toResponse);
    }

    public Mono<ChatRoomResponse> getRoomById(Long roomId) {
        return chatRoomRepository.findById(roomId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found")))
                .map(this::toResponse);
    }

    public Mono<Void> joinRoom(Long roomId, Long userId) {
        return chatRoomRepository.findById(roomId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found")))
                .then(chatRoomMemberRepository.existsByRoomIdAndUserId(roomId, userId))
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "Already a member of this room"));
                    }
                    ChatRoomMember member = ChatRoomMember.builder()
                            .roomId(roomId)
                            .userId(userId)
                            .build();
                    return chatRoomMemberRepository.save(member).then();
                });
    }

    private ChatRoomResponse toResponse(ChatRoom room) {
        return ChatRoomResponse.builder()
                .id(room.getId())
                .name(room.getName())
                .creatorId(room.getCreatorId())
                .createdAt(room.getCreatedAt())
                .build();
    }
}

