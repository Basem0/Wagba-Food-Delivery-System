package com.wagba.dto.wallet;

import java.math.BigDecimal;

public record WalletTransactionResponse(
        Long id,
        BigDecimal amount,
        String type,
        String description,
        String reference,
        String createdAt
) {
}
