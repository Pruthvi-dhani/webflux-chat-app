package org.chatapp.controller;

import lombok.RequiredArgsConstructor;
import org.chatapp.dto.AuthResponse;
import org.chatapp.dto.LoginRequest;
import org.chatapp.dto.RegisterRequest;
import org.chatapp.model.User;
import org.chatapp.repository.UserRepository;
import org.chatapp.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    @Autowired
    private final UserRepository userRepository;

    @Autowired
    private final PasswordEncoder passwordEncoder;

    @Autowired
    private final JwtUtil jwtUtil;

    @Autowired
    private final ReactiveAuthenticationManager authenticationManager;

    @PostMapping("/register")
    public Mono<AuthResponse> register(@RequestBody RegisterRequest request) {
        return userRepository.existsByUsername(request.getUsername())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.CONFLICT, "Username already taken"));
                    }
                    User user = User.builder()
                            .username(request.getUsername())
                            .password(passwordEncoder.encode(request.getPassword()))
                            .role("USER")
                            .build();
                    return userRepository.save(user);
                })
                .map(saved -> new AuthResponse(
                        jwtUtil.generateToken(saved.getUsername(), saved.getId()),
                        saved.getUsername(),
                        saved.getId()
                ));
    }

    @PostMapping("/login")
    public Mono<AuthResponse> login(@RequestBody LoginRequest request) {
        return authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(
                        request.getUsername(), request.getPassword()))
                .onErrorMap(ex -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid credentials"))
                .flatMap(auth -> userRepository.findByUsername(auth.getName()))
                .map(user -> new AuthResponse(
                        jwtUtil.generateToken(user.getUsername(), user.getId()),
                        user.getUsername(),
                        user.getId()
                ));
    }
}

