package org.chatapp.config;

import lombok.RequiredArgsConstructor;
import org.chatapp.handler.ChatMessageHandler;
import org.chatapp.handler.ChatRoomHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;

@Configuration
@RequiredArgsConstructor
public class RouterConfig {

    private final ChatRoomHandler chatRoomHandler;
    private final ChatMessageHandler chatMessageHandler;

    @Bean
    public RouterFunction<ServerResponse> chatRoutes() {
        return RouterFunctions.route()
                .POST("/api/rooms", chatRoomHandler::createRoom)
                .GET("/api/rooms", chatRoomHandler::listRooms)
                .GET("/api/rooms/{id}", chatRoomHandler::getRoom)
                .POST("/api/rooms/{id}/join", chatRoomHandler::joinRoom)
                .GET("/api/rooms/{id}/messages", chatMessageHandler::getMessages)
                .build();
    }
}

