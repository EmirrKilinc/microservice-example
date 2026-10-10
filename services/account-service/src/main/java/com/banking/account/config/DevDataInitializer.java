package com.banking.account.config;

import com.banking.account.entity.Account;
import com.banking.account.repository.AccountRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

// Yalnızca "dev" profili için: iki örnek hesap ekler. Şemayı da bu profilde Hibernate
// oluşturur (application-dev.yml > ddl-auto). Gerçek ortamda şema Flyway/Liquibase
// migration'ları ile, deploy hattında yönetilir.
@Component
@Profile("dev")
class DevDataInitializer implements ApplicationRunner {

    static final UUID SAMPLE_ACCOUNT_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    static final UUID SAMPLE_ACCOUNT_B = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private final AccountRepository accounts;

    DevDataInitializer(AccountRepository accounts) {
        this.accounts = accounts;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (accounts.count() > 0)
            return;

        accounts.saveAll(List.of(
                new Account(SAMPLE_ACCOUNT_A, "Ada Lovelace", "TR000000000000000000000001", "TRY", new BigDecimal("10000")),
                new Account(SAMPLE_ACCOUNT_B, "Alan Turing", "TR000000000000000000000002", "TRY", new BigDecimal("2500"))));
    }
}
