package com.banking.transfer.config;

import com.banking.contracts.events.TransferCompletedEvent;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Asenkron bağımlılık: RabbitMQ. Bu servis yalnızca yayınlar; kuyruk tanımlamaz,
// listener kaydı yoktur. Kuyruklar tüketici servislere aittir.
@Configuration
class MessagingConfig {

    @Bean
    FanoutExchange transferCompletedExchange() {
        return new FanoutExchange(TransferCompletedEvent.EXCHANGE);
    }

    // Event'ler tel üzerinde JSON'dır (varsayılan Java serialization değil).
    @Bean
    MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
