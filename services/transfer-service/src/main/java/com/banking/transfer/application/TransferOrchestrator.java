package com.banking.transfer.application;

import com.banking.contracts.events.TransferCompletedEvent;
import com.banking.contracts.grpc.AccountServiceGrpc;
import com.banking.contracts.grpc.DebitBalanceRequest;
import com.banking.transfer.contracts.CreateTransferRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

// Transfer akışının orkestrasyonu. İki iletişim tarzı bilinçli olarak ayrılmıştır:
//
//   1) Bakiye düşme  -> gRPC, SENKRON. Sonuca hemen ihtiyaç var: bakiye yetmiyorsa
//      transfer hiç başlamamalı ve kullanıcıya anında yanıt dönmeli.
//   2) "Transfer tamamlandı" -> RabbitMQ, ASENKRON. Bildirim gibi yan etkiler transferi
//      bekletmemeli; notification-service kapalı olsa bile transfer başarılı sayılır.
@Service
public class TransferOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(TransferOrchestrator.class);

    private final AccountServiceGrpc.AccountServiceBlockingStub accounts;
    private final RabbitTemplate publisher;

    public TransferOrchestrator(AccountServiceGrpc.AccountServiceBlockingStub accounts, RabbitTemplate publisher) {
        this.accounts = accounts;
        this.publisher = publisher;
    }

    public TransferResult execute(CreateTransferRequest request) {
        var transferId = UUID.randomUUID();

        // Deadline: Account Service yanıt vermezse bu istek sonsuza kadar asılı kalmaz;
        // bir servisteki yavaşlık zincirleme şekilde diğerlerine yayılmaz.
        var debit = accounts.withDeadlineAfter(5, TimeUnit.SECONDS).debitBalance(
                DebitBalanceRequest.newBuilder()
                        .setAccountId(request.fromAccountId().toString())
                        .setAmountMinor(request.amount().movePointRight(2).setScale(0, RoundingMode.HALF_EVEN).longValueExact())
                        .setCurrency(request.currency())
                        .setIdempotencyKey(transferId.toString())
                        .build());

        if (!debit.getSuccess()) {
            log.info("Transfer {} rejected: {}", transferId, debit.getFailureReason());
            return new TransferResult(transferId, false, debit.getFailureReason().name());
        }

        // İskelette eksik bırakılan iki konu:
        //  - Alıcı hesaba yatırma (credit) ve o adım başarısız olursa düşümü geri alma
        //    (compensation). Dağıtık transaction yoktur; bu bir Saga ile çözülür.
        //  - Dual write: bakiye düştü ama aşağıdaki publish başarısız olursa event kaybolur.
        //    Üretimde Transactional Outbox kullanılır.
        publisher.convertAndSend(
                TransferCompletedEvent.EXCHANGE,
                "",
                new TransferCompletedEvent(
                        transferId,
                        request.fromAccountId(),
                        request.toAccountId(),
                        request.amount(),
                        request.currency(),
                        Instant.now()));

        return new TransferResult(transferId, true, null);
    }
}
