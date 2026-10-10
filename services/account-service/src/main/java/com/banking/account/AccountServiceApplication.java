package com.banking.account;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Sınıf bilinçli olarak com.banking.account paketindedir; component taraması böylece
// tüm katman paketlerini (controller, grpc, service, repository, entity) kapsar.
//
// Aynı servis iki giriş kapısı sunar (portlar application.yml'de):
//   8080 / HTTP1 -> REST: Gateway üzerinden dış istemciler (north-south trafik)
//   8081 / HTTP2 -> gRPC: iç ağdaki servisler (east-west trafik)
// REST'i gömülü Tomcat, gRPC'yi ayrı bir Netty sunucusu dinler.
@SpringBootApplication
public class AccountServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountServiceApplication.class, args);
    }
}
