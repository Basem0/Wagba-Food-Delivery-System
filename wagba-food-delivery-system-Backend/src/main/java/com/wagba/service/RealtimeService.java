package com.wagba.service;

import com.wagba.realtime.RealtimeNotification;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class RealtimeService {

    private final SimpMessagingTemplate template;

    public RealtimeService(SimpMessagingTemplate template) {
        this.template = template;
    }

    public void notifyUser(String email, RealtimeNotification payload) {
        template.convertAndSendToUser(email, "/queue/notifications", payload);
    }

    public void toTopic(String destination, Object payload) {
        template.convertAndSend(destination, payload);
    }
}
