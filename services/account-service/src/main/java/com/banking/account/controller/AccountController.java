package com.banking.account.controller;

import com.banking.account.dto.BalanceDto;
import com.banking.account.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

// Sunum katmanı: dışa açık REST yüzeyi. Bilinçli olarak yalnızca OKUMA sunar: bakiye düşme
// dış dünyaya açık değildir, o işlem sadece iç gRPC kontratı üzerinden yapılabilir.
@RestController
@RequestMapping("/api/accounts")
class AccountController {

    private final AccountService accountService;

    AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/{id}/balance")
    ResponseEntity<BalanceDto> balance(@PathVariable UUID id) {
        return ResponseEntity.of(accountService.getBalance(id));
    }
}
