package com.banking.contracts.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

// Integration event: servis sınırını aşan, "olmuş bitmiş" bir olguyu anlatır (geçmiş zaman).
// Yayınlayan (transfer-service) kimin dinlediğini bilmez; yeni bir tüketici eklemek
// yayınlayan tarafta kod değişikliği gerektirmez.
//
// Tel üzerinde JSON olarak taşınır. Exchange adı da kontratın parçasıdır: publisher ve
// consumer aynı adı bu ortak modülden alır.
//
// Şema evrimi: alan eklemek güvenlidir; alan silmek/yeniden adlandırmak kırıcıdır.
public record TransferCompletedEvent(
        UUID transferId,
        UUID fromAccountId,
        UUID toAccountId,
        BigDecimal amount,
        String currency,
        Instant completedAtUtc) {

    // Fanout exchange: her tüketici servis kendi kuyruğunu buraya bağlar.
    public static final String EXCHANGE = "banking.events.transfer-completed";
}
