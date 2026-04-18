package org.chatapp.service;

import org.chatapp.dto.ChatRoomResponse;
import org.chatapp.model.ChatRoom;
import org.chatapp.model.ChatRoomMember;
import org.chatapp.repository.ChatRoomMemberRepository;
import org.chatapp.repository.ChatRoomRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @InjectMocks
    private ChatRoomService chatRoomService;

    private ChatRoom sampleRoom(Long id, String name, Long creatorId) {
        return ChatRoom.builder()
                .id(id)
                .name(name)
                .creatorId(creatorId)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createRoom_shouldSaveRoomAndAddMember() {
        ChatRoom saved = sampleRoom(1L, "General", 10L);
        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(Mono.just(saved));
        when(chatRoomMemberRepository.save(any(ChatRoomMember.class)))
                .thenReturn(Mono.just(ChatRoomMember.builder().id(1L).roomId(1L).userId(10L).build()));

        StepVerifier.create(chatRoomService.createRoom("General", 10L))
                .assertNext(response -> {
                    assertThat(response.getId()).isEqualTo(1L);
                    assertThat(response.getName()).isEqualTo("General");
                    assertThat(response.getCreatorId()).isEqualTo(10L);
                })
                .verifyComplete();

        verify(chatRoomRepository).save(any(ChatRoom.class));
        verify(chatRoomMemberRepository).save(any(ChatRoomMember.class));
    }

    @Test
    void createRoom_blankName_shouldError() {
        StepVerifier.create(chatRoomService.createRoom("  ", 10L))
                .expectErrorMatches(ex -> ex instanceof ResponseStatusException
                        && ((ResponseStatusException) ex).getStatusCode().value() == 400)
                .verify();
    }

    @Test
    void createRoom_nullName_shouldError() {
        StepVerifier.create(chatRoomService.createRoom(null, 10L))
                .expectErrorMatches(ex -> ex instanceof ResponseStatusException
                        && ((ResponseStatusException) ex).getStatusCode().value() == 400)
                .verify();
    }

    @Test
    void listRoomsForUser_shouldReturnUserRooms() {
        ChatRoomMember member1 = ChatRoomMember.builder().id(1L).roomId(1L).userId(10L).build();
        ChatRoomMember member2 = ChatRoomMember.builder().id(2L).roomId(2L).userId(10L).build();
        when(chatRoomMemberRepository.findByUserId(10L)).thenReturn(Flux.just(member1, member2));
        when(chatRoomRepository.findById(1L)).thenReturn(Mono.just(sampleRoom(1L, "Room1", 10L)));
        when(chatRoomRepository.findById(2L)).thenReturn(Mono.just(sampleRoom(2L, "Room2", 5L)));

        StepVerifier.create(chatRoomService.listRoomsForUser(10L))
                .assertNext(r -> assertThat(r.getName()).isEqualTo("Room1"))
                .assertNext(r -> assertThat(r.getName()).isEqualTo("Room2"))
                .verifyComplete();
    }

    @Test
    void listRoomsForUser_noRooms_shouldReturnEmpty() {
        when(chatRoomMemberRepository.findByUserId(10L)).thenReturn(Flux.empty());

        StepVerifier.create(chatRoomService.listRoomsForUser(10L))
                .verifyComplete();
    }

    @Test
    void getRoomById_found_shouldReturnRoom() {
        when(chatRoomRepository.findById(1L)).thenReturn(Mono.just(sampleRoom(1L, "General", 10L)));

        StepVerifier.create(chatRoomService.getRoomById(1L))
                .assertNext(r -> {
                    assertThat(r.getId()).isEqualTo(1L);
                    assertThat(r.getName()).isEqualTo("General");
                })
                .verifyComplete();
    }

    @Test
    void getRoomById_notFound_shouldReturn404() {
        when(chatRoomRepository.findById(99L)).thenReturn(Mono.empty());

        StepVerifier.create(chatRoomService.getRoomById(99L))
                .expectErrorMatches(ex -> ex instanceof ResponseStatusException
                        && ((ResponseStatusException) ex).getStatusCode().value() == 404)
                .verify();
    }

    @Test
    void joinRoom_shouldAddMember() {
        when(chatRoomRepository.findById(1L)).thenReturn(Mono.just(sampleRoom(1L, "General", 5L)));
        when(chatRoomMemberRepository.existsByRoomIdAndUserId(1L, 10L)).thenReturn(Mono.just(false));
        when(chatRoomMemberRepository.save(any(ChatRoomMember.class)))
                .thenReturn(Mono.just(ChatRoomMember.builder().id(1L).roomId(1L).userId(10L).build()));

        StepVerifier.create(chatRoomService.joinRoom(1L, 10L))
                .verifyComplete();

        verify(chatRoomMemberRepository).save(any(ChatRoomMember.class));
    }

    @Test
    void joinRoom_alreadyMember_shouldReturn409() {
        when(chatRoomRepository.findById(1L)).thenReturn(Mono.just(sampleRoom(1L, "General", 5L)));
        when(chatRoomMemberRepository.existsByRoomIdAndUserId(1L, 10L)).thenReturn(Mono.just(true));

        StepVerifier.create(chatRoomService.joinRoom(1L, 10L))
                .expectErrorMatches(ex -> ex instanceof ResponseStatusException
                        && ((ResponseStatusException) ex).getStatusCode().value() == 409)
                .verify();

        verify(chatRoomMemberRepository, never()).save(any());
    }

    @Test
    void joinRoom_roomNotFound_shouldReturn404() {
        when(chatRoomRepository.findById(99L)).thenReturn(Mono.empty());
        when(chatRoomMemberRepository.existsByRoomIdAndUserId(99L, 10L)).thenReturn(Mono.just(false));

        StepVerifier.create(chatRoomService.joinRoom(99L, 10L))
                .expectErrorMatches(ex -> ex instanceof ResponseStatusException
                        && ((ResponseStatusException) ex).getStatusCode().value() == 404)
                .verify();
    }
}


