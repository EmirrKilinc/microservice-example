package com.banking.account.dto;

import java.math.BigDecimal;

public record DebitResult(boolean success, DebitFailure failure, BigDecimal remainingBalance) {
}
