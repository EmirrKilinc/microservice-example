package com.banking.account.service;

import com.banking.account.dto.BalanceDto;
import com.banking.account.dto.DebitFailure;
import com.banking.account.dto.DebitResult;
import com.banking.account.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

// İş mantığı katmanı. Hem REST controller'ı hem gRPC servisi bu sınıfı çağırır;
// taşıma protokolü değişse de iş kuralları tek yerde kalır.
@Service
public class AccountService {

    private final AccountRepository accounts;

    public AccountService(AccountRepository accounts) {
        this.accounts = accounts;
    }

    public Optional<BalanceDto> getBalance(UUID accountId) {
        return accounts.findById(accountId)
                .map(account -> new BalanceDto(account.getId(), account.getBalance(), account.getCurrency()));
    }

    // İş kuralı reddi (yetersiz bakiye vb.) exception değil, dönüş değerinin parçasıdır.
    @Transactional
    public DebitResult debit(UUID accountId, BigDecimal amount, String currency, String idempotencyKey) {
        if (amount.signum() <= 0)
            throw new IllegalArgumentException("amount must be positive.");

        // İskelette eksik bırakıldı: idempotencyKey daha önce işlendiyse aynı sonucu
        // tekrar dönmek gerekir (işlenmiş anahtarlar tablosu). Aksi halde istemcinin
        // timeout sonrası retry'ı parayı iki kez düşer.
        var found = accounts.findById(accountId);
        if (found.isEmpty())
            return new DebitResult(false, DebitFailure.ACCOUNT_NOT_FOUND, BigDecimal.ZERO);

        var account = found.get();
        if (!account.getCurrency().equalsIgnoreCase(currency))
            return new DebitResult(false, DebitFailure.CURRENCY_MISMATCH, account.getBalance());
        if (account.getBalance().compareTo(amount) < 0)
            return new DebitResult(false, DebitFailure.INSUFFICIENT_FUNDS, account.getBalance());

        account.setBalance(account.getBalance().subtract(amount));
        accounts.save(account);
        return new DebitResult(true, DebitFailure.NONE, account.getBalance());
    }
}
