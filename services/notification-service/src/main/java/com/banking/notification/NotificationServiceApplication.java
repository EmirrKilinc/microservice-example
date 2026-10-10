package com.banking.notification;

import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

// Web uygulaması değil: hiçbir HTTP endpoint'i açılmaz. Süreci ayakta tutan,
// @RabbitListener'ların kuyruğu dinleyen thread'leridir.
@SpringBootApplication
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }

    // Event'ler tel üzerinde JSON'dır; hedef tip listener metodunun parametresinden çıkarılır.
    @Bean
    MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
