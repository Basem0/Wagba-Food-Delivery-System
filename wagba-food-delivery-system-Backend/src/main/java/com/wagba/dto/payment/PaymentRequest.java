package com.wagba.dto.payment;

import jakarta.validation.constraints.NotNull;

public class PaymentRequest {

    @NotNull(message = "orderId is required")
    private Long orderId;

    /** Stripe PaymentIntent id, sent back on confirm so the payment is traceable. */
    private String paymentReference;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }
}
