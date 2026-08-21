package com.wagba.service;

import com.wagba.entity.BlacklistedToken;
import com.wagba.repository.BlacklistedTokenRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class TokenBlacklistService {

    private final BlacklistedTokenRepository blacklistedTokenRepository;

    public TokenBlacklistService(BlacklistedTokenRepository blacklistedTokenRepository) {
        this.blacklistedTokenRepository = blacklistedTokenRepository;
    }

    public void blacklist(String value) {
        BlacklistedToken token = new BlacklistedToken(value, LocalDateTime.now().plusHours(24));
        blacklistedTokenRepository.save(token);
    }

    public boolean isBlacklisted(String value) {
        return blacklistedTokenRepository.existsById(value);
    }
}
