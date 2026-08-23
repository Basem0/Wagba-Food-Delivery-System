package com.wagba.dto.notification;

public record NotificationResponse(
        Long id,
        String type,
        String title,
        String message,
        Long orderId,
        boolean read,
        String createdAt
) {
}
