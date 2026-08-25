package com.wagba.service;

import com.wagba.dto.notification.NotificationResponse;
import com.wagba.entity.Notification;
import com.wagba.realtime.RealtimeNotification;
import com.wagba.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    public void save(String email, RealtimeNotification n) {
        if (email == null) return;
        Notification entity = new Notification();
        entity.setUserEmail(email);
        entity.setType(n.type());
        entity.setTitle(n.title());
        entity.setMessage(n.message());
        entity.setOrderId(n.orderId());
        repository.save(entity);
    }

    public Map<String, Object> list(String email, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        Page<Notification> p = repository.findByUserEmailOrderByCreatedAtDesc(email, pageable);
        List<NotificationResponse> content = p.getContent().stream().map(this::toResponse).toList();
        long unread = repository.countByUserEmailAndReadFalse(email);
        return Map.of(
                "content", content,
                "page", p.getNumber(),
                "size", p.getSize(),
                "totalElements", p.getTotalElements(),
                "totalPages", p.getTotalPages(),
                "unreadCount", unread
        );
    }

    public long unreadCount(String email) {
        return repository.countByUserEmailAndReadFalse(email);
    }

    public void markRead(Long id, String email) {
        repository.findById(id).ifPresent(n -> {
            if (email.equals(n.getUserEmail())) {
                n.setRead(true);
                repository.save(n);
            }
        });
    }

    @Transactional
    public void markAllRead(String email) {
        repository.markAllRead(email);
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getTitle(),
                n.getMessage(),
                n.getOrderId(),
                n.isRead(),
                n.getCreatedAt() != null ? n.getCreatedAt().toString() : null
        );
    }
}
