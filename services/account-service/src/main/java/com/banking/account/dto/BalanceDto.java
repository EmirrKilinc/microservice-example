package com.banking.account.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record BalanceDto(UUID accountId, BigDecimal balance, String currency) {
}
