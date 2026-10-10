package com.banking.transfer.web;

import com.banking.transfer.application.TransferOrchestrator;
import com.banking.transfer.contracts.CreateTransferRequest;
import com.banking.transfer.contracts.TransferResponse;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;

@RestController
class TransferController {

    private final TransferOrchestrator orchestrator;

    TransferController(TransferOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping("/api/transfers")
    ResponseEntity<?> create(@RequestBody CreateTransferRequest request) {
        if (!isValid(request))
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "All fields are required, amount must be positive and accounts must differ."));

        try {
            var result = orchestrator.execute(request);

            return result.completed()
                    ? ResponseEntity.created(URI.create("/api/transfers/" + result.transferId()))
                            .body(new TransferResponse(result.transferId(), "Completed", null))
                    // İstek geçerli ama iş kuralı reddetti (ör. yetersiz bakiye).
                    : ResponseEntity.unprocessableEntity()
                            .body(new TransferResponse(result.transferId(), "Rejected", result.reason()));
        } catch (StatusRuntimeException e) {
            var code = e.getStatus().getCode();
            if (code != Status.Code.UNAVAILABLE && code != Status.Code.DEADLINE_EXCEEDED)
                throw e;

            // Alt servis hatası istemciye iç detay sızdırmadan 503 olarak çevrilir.
            return ResponseEntity.of(ProblemDetail.forStatusAndDetail(
                    HttpStatus.SERVICE_UNAVAILABLE, "Account service is unavailable.")).build();
        }
    }

    private static boolean isValid(CreateTransferRequest request) {
        return request.fromAccountId() != null
                && request.toAccountId() != null
                && request.amount() != null
                && request.currency() != null
                && request.amount().signum() > 0
                && !request.fromAccountId().equals(request.toAccountId());
    }
}
