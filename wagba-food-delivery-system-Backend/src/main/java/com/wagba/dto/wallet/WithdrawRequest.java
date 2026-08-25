package com.wagba.dto.wallet;

import java.math.BigDecimal;

public class WithdrawRequest {

    private BigDecimal amount;

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
