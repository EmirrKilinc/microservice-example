package com.banking.account.grpc;

import com.banking.account.dto.DebitFailure;
import com.banking.account.service.AccountService;
import com.banking.contracts.grpc.AccountServiceGrpc;
import com.banking.contracts.grpc.DebitBalanceReply;
import com.banking.contracts.grpc.DebitBalanceRequest;
import com.banking.contracts.grpc.DebitFailureReason;
import com.banking.contracts.grpc.GetBalanceReply;
import com.banking.contracts.grpc.GetBalanceRequest;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.UUID;

// Sunum katmanının gRPC ayağı. İş mantığı içermez: protobuf mesajını çözer, AccountService'i
// çağırır, sonucu tekrar protobuf'a çevirir. REST controller'ı ile aynı service'i paylaşır.
@Service
class AccountGrpcService extends AccountServiceGrpc.AccountServiceImplBase {

    private final AccountService accountService;

    AccountGrpcService(AccountService accountService) {
        this.accountService = accountService;
    }

    @Override
    public void getBalance(GetBalanceRequest request, StreamObserver<GetBalanceReply> responseObserver) {
        var accountId = parseAccountId(request.getAccountId());
        if (accountId.isEmpty()) {
            responseObserver.onError(invalidAccountId());
            return;
        }

        var balance = accountService.getBalance(accountId.get());
        if (balance.isEmpty()) {
            responseObserver.onError(Status.NOT_FOUND.withDescription("Account not found.").asRuntimeException());
            return;
        }

        responseObserver.onNext(GetBalanceReply.newBuilder()
                .setAccountId(balance.get().accountId().toString())
                .setBalanceMinor(toMinor(balance.get().balance()))
                .setCurrency(balance.get().currency())
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void debitBalance(DebitBalanceRequest request, StreamObserver<DebitBalanceReply> responseObserver) {
        var accountId = parseAccountId(request.getAccountId());
        if (accountId.isEmpty()) {
            responseObserver.onError(invalidAccountId());
            return;
        }
        if (request.getAmountMinor() <= 0) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT.withDescription("amount_minor must be positive.").asRuntimeException());
            return;
        }

        var result = accountService.debit(
                accountId.get(), fromMinor(request.getAmountMinor()), request.getCurrency(), request.getIdempotencyKey());

        // İş kuralı reddi (yetersiz bakiye vb.) exception değil, yanıtın parçasıdır.
        responseObserver.onNext(DebitBalanceReply.newBuilder()
                .setSuccess(result.success())
                .setFailureReason(toReason(result.failure()))
                .setRemainingBalanceMinor(toMinor(result.remainingBalance()))
                .build());
        responseObserver.onCompleted();
    }

    private static DebitFailureReason toReason(DebitFailure failure) {
        return switch (failure) {
            case ACCOUNT_NOT_FOUND -> DebitFailureReason.DEBIT_FAILURE_REASON_ACCOUNT_NOT_FOUND;
            case INSUFFICIENT_FUNDS -> DebitFailureReason.DEBIT_FAILURE_REASON_INSUFFICIENT_FUNDS;
            case CURRENCY_MISMATCH -> DebitFailureReason.DEBIT_FAILURE_REASON_CURRENCY_MISMATCH;
            case NONE -> DebitFailureReason.DEBIT_FAILURE_REASON_UNSPECIFIED;
        };
    }

    private static Optional<UUID> parseAccountId(String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static RuntimeException invalidAccountId() {
        return Status.INVALID_ARGUMENT.withDescription("account_id must be a UUID.").asRuntimeException();
    }

    // Tel üzerinde kuruş (int64), serviste BigDecimal. Dönüşüm yalnızca bu sınırda yapılır.
    private static long toMinor(BigDecimal amount) {
        return amount.movePointRight(2).setScale(0, RoundingMode.HALF_EVEN).longValueExact();
    }

    private static BigDecimal fromMinor(long minor) {
        return BigDecimal.valueOf(minor, 2);
    }
}
