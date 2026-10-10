package com.banking.transfer.contracts;

import java.util.UUID;

public record TransferResponse(UUID transferId, String status, String reason) {
}
