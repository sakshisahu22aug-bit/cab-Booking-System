package com.labmentix.cabbooking.config;

import com.labmentix.cabbooking.config.security.JwtTokenProvider;
import com.labmentix.cabbooking.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration enabling STOMP messaging for real-time ride tracking and driver coordinates.
 * Includes JWT channel interceptor to authenticate incoming WebSocket connections.
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    @Value("${app.websocket.endpoint:/ws}")
    private String websocketEndpoint;

    @Value("${app.websocket.topic-prefix:/topic}")
    private String topicPrefix;

    @Value("${app.websocket.app-destination-prefix:/app}")
    private String appDestinationPrefix;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enable simple memory-based broker for broadcast and targeted queues
        registry.enableSimpleBroker(topicPrefix, "/queue");
        registry.setApplicationDestinationPrefixes(appDestinationPrefix);
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // STOMP endpoint with SockJS fallback support
        registry.addEndpoint(websocketEndpoint)
                .setAllowedOriginPatterns("*")
                .withSockJS();

        // Native WebSocket STOMP endpoint for mobile and standard clients
        registry.addEndpoint(websocketEndpoint)
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");

                    if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7).trim();
                        try {
                            if (jwtTokenProvider.validateToken(token)) {
                                String username = jwtTokenProvider.extractUsername(token);
                                UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

                                UsernamePasswordAuthenticationToken auth =
                                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                                accessor.setUser(auth);
                                SecurityContextHolder.getContext().setAuthentication(auth);
                                log.debug("Authenticated WebSocket connection for user: {}", username);
                            }
                        } catch (Exception ex) {
                            log.error("Failed to authenticate WebSocket user from JWT: {}", ex.getMessage());
                        }
                    }
                }
                return message;
            }
        });
    }
}
