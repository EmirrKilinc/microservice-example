package com.banking.transfer.contracts;

import java.math.BigDecimal;
import java.util.UUID;

// Bu servisin dışa açık HTTP kontratı. banking-contracts'a konmaz: orası servisler ARASI
// kontratlar içindir, bu ise yalnızca transfer-service'in kendi API'sine aittir.
public record CreateTransferRequest(UUID fromAccountId, UUID toAccountId, BigDecimal amount, String currency) {
}
