package com.wagba.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "blacklisted_tokens")
public class BlacklistedToken {

    @Id
    private String jti;

    @Column(nullable = false)
    private LocalDateTime expiry;

    public BlacklistedToken() {
    }

    public BlacklistedToken(String jti, LocalDateTime expiry) {
        this.jti = jti;
        this.expiry = expiry;
    }

    public String getJti() {
        return jti;
    }

    public void setJti(String jti) {
        this.jti = jti;
    }

    public LocalDateTime getExpiry() {
        return expiry;
    }

    public void setExpiry(LocalDateTime expiry) {
        this.expiry = expiry;
    }
}
