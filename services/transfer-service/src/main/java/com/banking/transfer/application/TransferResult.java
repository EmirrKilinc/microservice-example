package com.banking.transfer.application;

import java.util.UUID;

public record TransferResult(UUID transferId, boolean completed, String reason) {
}
