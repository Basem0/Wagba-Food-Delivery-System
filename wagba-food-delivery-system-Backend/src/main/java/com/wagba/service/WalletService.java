package com.wagba.service;

import com.wagba.dto.wallet.WalletResponse;
import com.wagba.dto.wallet.WalletTransactionResponse;
import com.wagba.entity.User;
import com.wagba.entity.Wallet;
import com.wagba.entity.WalletTransaction;
import com.wagba.entity.enums.WalletTxnType;
import com.wagba.repository.UserRepository;
import com.wagba.repository.WalletRepository;
import com.wagba.repository.WalletTransactionRepository;
import com.wagba.security.SecurityUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final UserRepository userRepository;
    private final StripeService stripeService;

    public WalletService(WalletRepository walletRepository,
                         WalletTransactionRepository walletTransactionRepository,
                         UserRepository userRepository,
                         StripeService stripeService) {
        this.walletRepository = walletRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.userRepository = userRepository;
        this.stripeService = stripeService;
    }

    private User currentUser() {
        return userRepository.findByEmail(SecurityUtil.getCurrentUserEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional
    public Wallet getOrCreateWallet(User user) {
        return walletRepository.findByUser(user)
                .orElseGet(() -> {
                    Wallet w = new Wallet();
                    w.setUser(user);
                    w.setBalance(BigDecimal.ZERO);
                    return walletRepository.save(w);
                });
    }

    @Transactional
    public void credit(User user, BigDecimal amount, String description, String reference) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return;
        Wallet wallet = getOrCreateWallet(user);
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        WalletTransaction txn = new WalletTransaction();
        txn.setWallet(wallet);
        txn.setAmount(amount);
        txn.setType(WalletTxnType.CREDIT);
        txn.setDescription(description);
        txn.setReference(reference);
        walletTransactionRepository.save(txn);
    }

    public WalletResponse getWalletInfo(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Wallet wallet = getOrCreateWallet(user);
        Page<WalletTransaction> txns = walletTransactionRepository
                .findByWalletIdOrderByCreatedAtDesc(wallet.getId(), PageRequest.of(0, 20));
        List<WalletTransactionResponse> list = txns.getContent().stream().map(t -> new WalletTransactionResponse(
                t.getId(), t.getAmount(), t.getType().name(), t.getDescription(), t.getReference(),
                t.getCreatedAt() != null ? t.getCreatedAt().toString() : null
        )).toList();
        return new WalletResponse(wallet.getBalance(), list);
    }

    @Transactional
    public void debit(User user, BigDecimal amount, String description, String reference) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return;
        Wallet wallet = getOrCreateWallet(user);
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient wallet balance");
        }
        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);

        WalletTransaction txn = new WalletTransaction();
        txn.setWallet(wallet);
        txn.setAmount(amount);
        txn.setType(WalletTxnType.DEBIT);
        txn.setDescription(description);
        txn.setReference(reference);
        walletTransactionRepository.save(txn);
    }

    @Transactional
    public Map<String, Object> withdraw(String email, BigDecimal amount) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Enter a valid amount");
        }
        debit(user, amount, "Withdrawal via Stripe", "STRIPE_PAYOUT");
        return stripeService.createPayout(amount);
    }

    public WalletResponse myWallet() {
        return getWalletInfo(currentUser().getEmail());
    }

    public java.util.List<java.util.Map<String, Object>> getEarningsChart(String email, int days) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Wallet wallet = getOrCreateWallet(user);
        java.time.LocalDateTime since = java.time.LocalDateTime.now().minusDays(days);
        List<WalletTransaction> txns = walletTransactionRepository
                .findByWalletIdOrderByCreatedAtDesc(wallet.getId(), PageRequest.of(0, 500))
                .getContent();
        java.util.Map<String, java.math.BigDecimal> daily = new java.util.LinkedHashMap<>();
        for (java.time.LocalDate d = java.time.LocalDate.now().minusDays(days - 1);
             !d.isAfter(java.time.LocalDate.now()); d = d.plusDays(1)) {
            daily.put(d.toString(), java.math.BigDecimal.ZERO);
        }
        for (WalletTransaction t : txns) {
            if (t.getType() == WalletTxnType.CREDIT && t.getCreatedAt() != null && t.getCreatedAt().isAfter(since)) {
                String day = t.getCreatedAt().toLocalDate().toString();
                daily.merge(day, t.getAmount(), java.math.BigDecimal::add);
            }
        }
        return daily.entrySet().stream().map(e -> {
            java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("date", e.getKey());
            m.put("amount", e.getValue());
            return m;
        }).toList();
    }
}
