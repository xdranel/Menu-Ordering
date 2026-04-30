package menuorderingapp.project.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${app.cors.allowed-origins:*}")
    private String allowedOrigins;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        boolean wildcard = allowedOrigins.contains("*");
        String[] origins = wildcard ? new String[]{"*"} : allowedOrigins.split(",");

        if (wildcard) {
            registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
            registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
        } else {
            registry.addEndpoint("/ws").setAllowedOrigins(origins).withSockJS();
            registry.addEndpoint("/ws").setAllowedOrigins(origins);
        }
    }
}
