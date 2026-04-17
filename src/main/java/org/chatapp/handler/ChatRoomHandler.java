package org.chatapp.handler;

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
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRoomHandler {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    /**
     * POST /api/rooms — create a room and auto-add the creator as a member.
     */
    public Mono<ServerResponse> createRoom(ServerRequest request) {
        return extractUserId()
                .flatMap(userId -> request.bodyToMono(CreateRoomRequest.class)
                        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required")))
                        .flatMap(body -> {
                            if (body.getName() == null || body.getName().isBlank()) {
                                return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Room name is required"));
                            }
                            ChatRoom room = ChatRoom.builder()
                                    .name(body.getName().trim())
                                    .creatorId(userId)
                                    .build();
                            return chatRoomRepository.save(room);
                        })
                        .flatMap(savedRoom -> {
                            ChatRoomMember member = ChatRoomMember.builder()
                                    .roomId(savedRoom.getId())
                                    .userId(userId)
                                    .build();
                            return chatRoomMemberRepository.save(member)
                                    .thenReturn(savedRoom);
                        })
                )
                .flatMap(room -> ServerResponse.status(HttpStatus.CREATED).bodyValue(toResponse(room)));
    }

    /**
     * GET /api/rooms — list all rooms the authenticated user is a member of.
     */
    public Mono<ServerResponse> listRooms(ServerRequest request) {
        return extractUserId()
                .flatMapMany(userId -> chatRoomMemberRepository.findByUserId(userId)
                        .flatMap(member -> chatRoomRepository.findById(member.getRoomId()))
                )
                .map(this::toResponse)
                .collectList()
                .flatMap(rooms -> ServerResponse.ok().bodyValue(rooms));
    }

    /**
     * GET /api/rooms/{id} — get room details.
     */
    public Mono<ServerResponse> getRoom(ServerRequest request) {
        Long roomId = Long.parseLong(request.pathVariable("id"));
        return chatRoomRepository.findById(roomId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found")))
                .flatMap(room -> ServerResponse.ok().bodyValue(toResponse(room)));
    }

    /**
     * POST /api/rooms/{id}/join — add current user to room members.
     */
    public Mono<ServerResponse> joinRoom(ServerRequest request) {
        Long roomId = Long.parseLong(request.pathVariable("id"));
        return extractUserId()
                .flatMap(userId -> chatRoomRepository.findById(roomId)
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
                            return chatRoomMemberRepository.save(member);
                        })
                )
                .flatMap(member -> ServerResponse.ok().bodyValue("Joined room successfully"));
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

