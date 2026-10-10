package com.banking.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Sistemin dışa açık TEK giriş noktası. Route tanımları koda değil yapılandırmaya
// (application.yml > spring.cloud.gateway) aittir; yeni bir servis eklemek
// yeniden derleme gerektirmez.
//
// Kimlik doğrulama, rate limiting, CORS gibi kesişen konular (cross-cutting concerns)
// servislere tek tek değil buraya eklenir.
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
