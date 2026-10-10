package com.banking.transfer.config;

import com.banking.contracts.grpc.AccountServiceGrpc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

// Senkron bağımlılık: Account Service'in gRPC istemcisi. Kanalın adresi koda değil
// yapılandırmaya aittir (application.yml > spring.grpc.client.channels.account).
// Üretimde buraya retry/circuit breaker politikası eklenir.
@Configuration
class AccountClientConfig {

    @Bean
    AccountServiceGrpc.AccountServiceBlockingStub accountClient(GrpcChannelFactory channels) {
        return AccountServiceGrpc.newBlockingStub(channels.createChannel("account"));
    }
}
