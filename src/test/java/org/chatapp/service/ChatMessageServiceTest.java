package org.chatapp.service;

import org.chatapp.dto.ChatMessageResponse;
import org.chatapp.model.ChatMessage;
import org.chatapp.model.User;
import org.chatapp.repository.ChatMessageRepository;
import org.chatapp.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ChatMessageService chatMessageService;

    private ChatMessage sampleMessage(Long id, Long roomId, Long senderId, String content) {
        return ChatMessage.builder()
                .id(id)
                .roomId(roomId)
                .senderId(senderId)
                .content(content)
                .type(ChatMessage.Type.CHAT)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private User sampleUser(Long id, String username) {
        return User.builder().id(id).username(username).build();
    }

    @Test
    void getMessages_shouldReturnMessagesWithSenderUsername() {
        ChatMessage msg1 = sampleMessage(1L, 1L, 10L, "Hello");
        ChatMessage msg2 = sampleMessage(2L, 1L, 20L, "Hi there");

        when(chatMessageRepository.findByRoomIdPaginated(1L, 50, 0))
                .thenReturn(Flux.just(msg1, msg2));
        when(userRepository.findById(10L)).thenReturn(Mono.just(sampleUser(10L, "alice")));
        when(userRepository.findById(20L)).thenReturn(Mono.just(sampleUser(20L, "bob")));

        StepVerifier.create(chatMessageService.getMessagesByRoomId(1L, 0, 50))
                .assertNext(r -> {
                    assertThat(r.getContent()).isEqualTo("Hello");
                    assertThat(r.getSenderUsername()).isEqualTo("alice");
                })
                .assertNext(r -> {
                    assertThat(r.getContent()).isEqualTo("Hi there");
                    assertThat(r.getSenderUsername()).isEqualTo("bob");
                })
                .verifyComplete();
    }

    @Test
    void getMessages_unknownSender_shouldReturnUnknownUsername() {
        ChatMessage msg = sampleMessage(1L, 1L, 999L, "Ghost message");

        when(chatMessageRepository.findByRoomIdPaginated(1L, 50, 0))
                .thenReturn(Flux.just(msg));
        when(userRepository.findById(999L)).thenReturn(Mono.empty());

        StepVerifier.create(chatMessageService.getMessagesByRoomId(1L, 0, 50))
                .assertNext(r -> {
                    assertThat(r.getContent()).isEqualTo("Ghost message");
                    assertThat(r.getSenderUsername()).isEqualTo("unknown");
                })
                .verifyComplete();
    }

    @Test
    void getMessages_emptyRoom_shouldReturnEmpty() {
        when(chatMessageRepository.findByRoomIdPaginated(1L, 50, 0))
                .thenReturn(Flux.empty());

        StepVerifier.create(chatMessageService.getMessagesByRoomId(1L, 0, 50))
                .verifyComplete();
    }

    @Test
    void getMessages_pagination_shouldCalculateOffset() {
        when(chatMessageRepository.findByRoomIdPaginated(1L, 20, 40))
                .thenReturn(Flux.empty());

        // page=2, size=20 → offset=40
        StepVerifier.create(chatMessageService.getMessagesByRoomId(1L, 2, 20))
                .verifyComplete();
    }
}

