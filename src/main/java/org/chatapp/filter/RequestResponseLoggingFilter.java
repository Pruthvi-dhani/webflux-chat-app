package org.chatapp.filter;

import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestResponseLoggingFilter implements WebFilter {

    private static final String CORRELATION_HEADER = "X-Correlation-Id";
    private static final String MASKED = "*****";

    private static final Set<String> SENSITIVE_HEADERS = Set.of("authorization", "cookie", "set-cookie");

    // Patterns to mask JSON fields: "password":"...", "token":"...", "refreshToken":"..."
    private static final Pattern SENSITIVE_BODY_PATTERN = Pattern.compile(
            "(\"(?:password|token|refreshToken)\"\\s*:\\s*)\"[^\"]*\"",
            Pattern.CASE_INSENSITIVE
    );

    private static final Set<String> SKIP_PREFIXES = Set.of("/actuator", "/ws/");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Skip WebSocket upgrades and actuator
        if (SKIP_PREFIXES.stream().anyMatch(path::startsWith) || isWebSocketUpgrade(request)) {
            return chain.filter(exchange);
        }

        String correlationId = request.getHeaders().getFirst(CORRELATION_HEADER);
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString().substring(0, 8);
        }

        String method = request.getMethod().name();
        String uri = request.getURI().toString();
        String maskedHeaders = maskHeaders(request.getHeaders());

        final String corrId = correlationId;

        // Buffer request body, log it, then replay
        return DataBufferUtils.join(request.getBody())
                .defaultIfEmpty(DefaultDataBufferFactory.sharedInstance.wrap(new byte[0]))
                .flatMap(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);

                    String requestBody = new String(bytes, StandardCharsets.UTF_8);
                    String maskedRequestBody = maskBody(requestBody);

                    log.info("[{}] >>> {} {} | Headers: {} | Body: {}",
                            corrId, method, uri, maskedHeaders,
                            maskedRequestBody.isEmpty() ? "<empty>" : maskedRequestBody);

                    // Replay the buffered body
                    ServerHttpRequest decoratedRequest = new ServerHttpRequestDecorator(request) {
                        @Override
                        public Flux<DataBuffer> getBody() {
                            DataBufferFactory factory = exchange.getResponse().bufferFactory();
                            return Flux.just(factory.wrap(bytes));
                        }
                    };

                    // Wrap response to capture body
                    ServerHttpResponse originalResponse = exchange.getResponse();
                    DataBufferFactory bufferFactory = originalResponse.bufferFactory();

                    ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
                        @Override
                        public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                            return DataBufferUtils.join(Flux.from(body))
                                    .flatMap(buffer -> {
                                        byte[] respBytes = new byte[buffer.readableByteCount()];
                                        buffer.read(respBytes);
                                        DataBufferUtils.release(buffer);

                                        String responseBody = new String(respBytes, StandardCharsets.UTF_8);
                                        String maskedResponseBody = maskBody(responseBody);
                                        String maskedRespHeaders = maskHeaders(getHeaders());

                                        log.info("[{}] <<< {} {} | Status: {} | Headers: {} | Body: {}",
                                                corrId, method, uri,
                                                getStatusCode(),
                                                maskedRespHeaders,
                                                maskedResponseBody.isEmpty() ? "<empty>" : maskedResponseBody);

                                        return super.writeWith(Flux.just(bufferFactory.wrap(respBytes)));
                                    });
                        }

                        @Override
                        public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
                            return writeWith(Flux.from(body).flatMapSequential(Flux::from));
                        }
                    };

                    ServerWebExchange mutatedExchange = exchange.mutate()
                            .request(decoratedRequest)
                            .response(decoratedResponse)
                            .build();

                    return chain.filter(mutatedExchange);
                });
    }

    private boolean isWebSocketUpgrade(ServerHttpRequest request) {
        String upgrade = request.getHeaders().getFirst(HttpHeaders.UPGRADE);
        return "websocket".equalsIgnoreCase(upgrade);
    }

    private String maskHeaders(HttpHeaders headers) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (var entry : headers.entrySet()) {
            if (!first) sb.append(", ");
            first = false;
            String key = entry.getKey();
            if (SENSITIVE_HEADERS.contains(key.toLowerCase())) {
                sb.append(key).append("=").append(MASKED);
            } else {
                sb.append(key).append("=").append(entry.getValue());
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private String maskBody(String body) {
        if (body == null || body.isEmpty()) {
            return "";
        }
        return SENSITIVE_BODY_PATTERN.matcher(body).replaceAll("$1\"" + MASKED + "\"");
    }
}

