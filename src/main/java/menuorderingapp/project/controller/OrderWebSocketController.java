package menuorderingapp.project.controller;

import menuorderingapp.project.model.dto.OrderResponse;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class OrderWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    public OrderWebSocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcastOrderUpdate(OrderResponse order) {
        messagingTemplate.convertAndSend("/topic/orders", order);
    }

    public void broadcastDashboardUpdate() {
        messagingTemplate.convertAndSend("/topic/dashboard", "refresh");
    }

    @MessageMapping("/ping")
    @SendTo("/topic/pong")
    public String handlePing(String message) {
        return "pong: " + message;
    }
}
