package com.banking.account.dto;

public enum DebitFailure {
    NONE,
    ACCOUNT_NOT_FOUND,
    INSUFFICIENT_FUNDS,
    CURRENCY_MISMATCH
}
