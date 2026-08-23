package com.wagba.realtime;

public record RealtimeNotification(
        String type,
        String title,
        String message,
        Long orderId,
        Object data
) {
    public RealtimeNotification(String type, String title, String message, Long orderId) {
        this(type, title, message, orderId, null);
    }
}
