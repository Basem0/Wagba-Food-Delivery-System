package com.wagba.repository;

import com.wagba.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByUserEmailOrderByCreatedAtDesc(String userEmail, Pageable pageable);
    long countByUserEmailAndReadFalse(String userEmail);
    void deleteByUserEmail(String userEmail);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.userEmail = :email")
    int markAllRead(@Param("email") String email);
}
