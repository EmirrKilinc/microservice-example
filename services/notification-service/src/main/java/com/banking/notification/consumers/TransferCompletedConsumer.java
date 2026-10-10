package com.banking.notification.consumers;

import com.banking.contracts.events.TransferCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

// transfer-service'i tanımaz; yalnızca ortak event kontratını bilir.
// Teslimat "at-least-once"tır: aynı event birden fazla kez gelebilir. Gerçek bir
// SMS/e-posta gönderiminde transferId ile tekrar kontrolü (idempotent consumer) gerekir.
@Component
class TransferCompletedConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransferCompletedConsumer.class);

    // Kuyruğu ve exchange binding'ini açılışta oluşturur. Kuyruk bu servise aittir:
    // servisin birden çok kopyası aynı kuyruğu paylaşır (competing consumers), yani
    // her event yalnızca bir kopya tarafından işlenir.
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "notification-service.transfer-completed", durable = "true"),
            exchange = @Exchange(name = TransferCompletedEvent.EXCHANGE, type = ExchangeTypes.FANOUT)))
    void consume(TransferCompletedEvent transfer) {
        // Simülasyon: gerçek bir bildirim sağlayıcısı yerine konsola log.
        log.info("[NOTIFICATION] Transfer {} tamamlandı: {} {}, {} -> {} ({})",
                transfer.transferId(),
                transfer.amount(),
                transfer.currency(),
                transfer.fromAccountId(),
                transfer.toAccountId(),
                transfer.completedAtUtc());

        // Exception fırlatılırsa mesaj reddedilir ve yeniden kuyruğa ALINMAZ
        // (application.yml > default-requeue-rejected). Üretimde dead-letter kuyruğu eklenir.
    }
}
