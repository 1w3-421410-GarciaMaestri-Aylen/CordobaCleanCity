package com.example.garbagereporting.messaging.producer;

import com.example.garbagereporting.config.ApplicationProperties;
import com.example.garbagereporting.messaging.event.ReportProcessingEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReportProcessingProducer {

    private final RabbitTemplate rabbitTemplate;
    private final ApplicationProperties properties;

    public void publish(ReportProcessingEvent event) {
        rabbitTemplate.convertAndSend(
                properties.getMessaging().getReportProcessingExchange(),
                properties.getMessaging().getReportProcessingRoutingKey(),
                event
        );
    }
}
