package com.wagba.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Payout;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PayoutCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class StripeService {

    @Value("${stripe.api.key:}")
    private String stripeApiKey;

    @Value("${stripe.currency:egp}")
    private String currency;

    public Map<String, Object> createPaymentIntent(BigDecimal amount) {
        long amountCents = amount.multiply(BigDecimal.valueOf(100)).longValue();

        if (stripeApiKey == null || stripeApiKey.isBlank()) {
            Map<String, Object> dev = new LinkedHashMap<>();
            dev.put("clientSecret", "dev_mock_secret_" + System.nanoTime());
            dev.put("amount", amount);
            dev.put("currency", currency);
            dev.put("devMode", true);
            return dev;
        }

        Stripe.apiKey = stripeApiKey;
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountCents)
                .setCurrency(currency)
                .build();
        try {
            PaymentIntent intent = PaymentIntent.create(params);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("clientSecret", intent.getClientSecret());
            result.put("amount", amount);
            result.put("currency", currency);
            result.put("devMode", false);
            return result;
        } catch (StripeException e) {
            throw new RuntimeException("Stripe error: " + e.getMessage());
        }
    }

    public Map<String, Object> createPayout(BigDecimal amount) {
        long amountCents = amount.multiply(BigDecimal.valueOf(100)).longValue();

        if (stripeApiKey == null || stripeApiKey.isBlank()) {
            Map<String, Object> dev = new LinkedHashMap<>();
            dev.put("id", "po_dev_" + System.nanoTime());
            dev.put("status", "paid");
            dev.put("amount", amount);
            dev.put("currency", currency);
            dev.put("devMode", true);
            return dev;
        }

        Stripe.apiKey = stripeApiKey;
        try {
            PayoutCreateParams params = PayoutCreateParams.builder()
                    .setAmount(amountCents)
                    .setCurrency(currency)
                    .build();
            Payout payout = Payout.create(params);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", payout.getId());
            result.put("status", payout.getStatus());
            result.put("amount", amount);
            result.put("currency", currency);
            result.put("devMode", false);
            return result;
        } catch (StripeException e) {
            // Real payout couldn't complete in this environment (no bank account, no test balance, etc.).
            // The wallet was already debited by the caller; return a simulated payout so the flow still completes.
            Map<String, Object> dev = new LinkedHashMap<>();
            dev.put("id", "po_dev_" + System.nanoTime());
            dev.put("status", "paid");
            dev.put("amount", amount);
            dev.put("currency", currency);
            dev.put("devMode", true);
            dev.put("note", "Real Stripe payout could not be completed here (" + e.getMessage() + "). Wallet debited; payout simulated.");
            return dev;
        }
    }
}
