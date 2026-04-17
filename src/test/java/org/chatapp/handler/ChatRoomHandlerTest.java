package org.chatapp.handler;

import org.chatapp.dto.AuthResponse;
import org.chatapp.dto.ChatRoomResponse;
import org.chatapp.dto.CreateRoomRequest;
import org.chatapp.dto.RegisterRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatRoomHandlerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private DatabaseClient databaseClient;

    private String jwtToken;
    private Long roomId;

    @BeforeAll
    void setup() {
        // Clean tables in reverse FK order
        databaseClient.sql("DELETE FROM chat_messages").then().block();
        databaseClient.sql("DELETE FROM chat_room_members").then().block();
        databaseClient.sql("DELETE FROM chat_rooms").then().block();
        databaseClient.sql("DELETE FROM users").then().block();

        // Register a user and get a JWT
        RegisterRequest register = new RegisterRequest();
        register.setUsername("testuser");
        register.setPassword("testpass123");

        AuthResponse authResponse = webTestClient.post()
                .uri("/api/auth/register")
                .bodyValue(register)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(authResponse).isNotNull();
        jwtToken = authResponse.getToken();
    }

    @Test
    @Order(1)
    void createRoom_shouldReturn201() {
        CreateRoomRequest request = new CreateRoomRequest();
        request.setName("General");

        ChatRoomResponse response = webTestClient.post()
                .uri("/api/rooms")
                .header("Authorization", "Bearer " + jwtToken)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ChatRoomResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("General");
        assertThat(response.getId()).isNotNull();

        roomId = response.getId();
    }

    @Test
    @Order(2)
    void listRooms_shouldReturnCreatedRoom() {
        List<ChatRoomResponse> rooms = webTestClient.get()
                .uri("/api/rooms")
                .header("Authorization", "Bearer " + jwtToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<ChatRoomResponse>>() {})
                .returnResult()
                .getResponseBody();

        assertThat(rooms).isNotNull();
        assertThat(rooms).hasSize(1);
        assertThat(rooms.getFirst().getName()).isEqualTo("General");
    }

    @Test
    @Order(3)
    void getRoom_shouldReturnDetails() {
        webTestClient.get()
                .uri("/api/rooms/{id}", roomId)
                .header("Authorization", "Bearer " + jwtToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ChatRoomResponse.class)
                .value(room -> {
                    assertThat(room.getId()).isEqualTo(roomId);
                    assertThat(room.getName()).isEqualTo("General");
                });
    }

    @Test
    @Order(4)
    void getRoom_notFound_shouldReturn404() {
        webTestClient.get()
                .uri("/api/rooms/{id}", 99999)
                .header("Authorization", "Bearer " + jwtToken)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @Order(5)
    void joinRoom_alreadyMember_shouldReturn409() {
        // Creator was auto-added, so joining again should conflict
        webTestClient.post()
                .uri("/api/rooms/{id}/join", roomId)
                .header("Authorization", "Bearer " + jwtToken)
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    @Order(6)
    void joinRoom_newUser_shouldSucceed() {
        // Register a second user
        RegisterRequest register = new RegisterRequest();
        register.setUsername("testuser2");
        register.setPassword("testpass123");

        AuthResponse auth = webTestClient.post()
                .uri("/api/auth/register")
                .bodyValue(register)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(auth).isNotNull();

        // Join the room with the second user
        webTestClient.post()
                .uri("/api/rooms/{id}/join", roomId)
                .header("Authorization", "Bearer " + auth.getToken())
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @Order(7)
    void getMessages_empty_shouldReturnEmptyList() {
        webTestClient.get()
                .uri("/api/rooms/{id}/messages?page=0&size=50", roomId)
                .header("Authorization", "Bearer " + jwtToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(0);
    }

    @Test
    @Order(8)
    void createRoom_withoutAuth_shouldReturn401() {
        CreateRoomRequest request = new CreateRoomRequest();
        request.setName("Secret Room");

        webTestClient.post()
                .uri("/api/rooms")
                .bodyValue(request)
                .exchange()
                .expectStatus().isUnauthorized();
    }
}

