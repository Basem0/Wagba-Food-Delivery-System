package com.wagba.security;

import io.jsonwebtoken.JwtException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JwtWsInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    public JwtWsInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            List<String> tokens = accessor.getNativeHeader("token");
            String token = (tokens != null && !tokens.isEmpty()) ? tokens.get(0) : null;

            if (token != null && jwtUtil.validateToken(token)) {
                try {
                    String email = jwtUtil.extractEmail(token);
                    String role = jwtUtil.extractRole(token);
                    User principal = new User(email,
                            "",
                            List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                    accessor.setUser(new UsernamePasswordAuthenticationToken(
                            principal, null, principal.getAuthorities()));
                } catch (JwtException | IllegalArgumentException e) {
                    accessor.setUser(null);
                }
            }
        }
        return message;
    }
}
