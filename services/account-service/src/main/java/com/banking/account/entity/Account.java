package com.banking.account.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.util.UUID;

// Entity katmanı: "accounts" tablosunun satırı. Yalnızca veri taşır; iş kuralları
// (yetersiz bakiye, para birimi kontrolü) service katmanındaki AccountService'tedir.
@Entity
@Table(name = "accounts")
public class Account {

    @Id
    private UUID id;

    @Column(name = "owner_name", length = 200, nullable = false)
    private String ownerName;

    @Column(length = 34, nullable = false, unique = true)
    private String iban;

    @Column(length = 3, nullable = false)
    private String currency;

    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal balance;

    // Optimistic concurrency token'ı. Aynı hesaba eşzamanlı iki düşüm gelirse
    // ikincisi eski sürümle yazmaya çalışır ve reddedilir (kayıp güncelleme olmaz):
    // UPDATE ... WHERE version = @eski değer. null = henüz kaydedilmemiş hesap.
    @Version
    private Long version;

    // JPA için.
    protected Account() {
    }

    public Account(UUID id, String ownerName, String iban, String currency, BigDecimal balance) {
        this.id = id;
        this.ownerName = ownerName;
        this.iban = iban;
        this.currency = currency;
        this.balance = balance;
    }

    public UUID getId() {
        return id;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getIban() {
        return iban;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
