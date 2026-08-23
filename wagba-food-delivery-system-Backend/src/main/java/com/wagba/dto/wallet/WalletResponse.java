package com.wagba.dto.wallet;

import java.math.BigDecimal;
import java.util.List;

public record WalletResponse(
        BigDecimal balance,
        List<WalletTransactionResponse> transactions
) {
}
