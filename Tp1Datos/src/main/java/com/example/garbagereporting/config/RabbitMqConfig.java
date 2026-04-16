package com.example.garbagereporting.config;

import java.util.Map;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    public SimpleMessageConverter rabbitMessageConverter() {
        SimpleMessageConverter converter = new SimpleMessageConverter();
        converter.addAllowedListPatterns(
                "com.example.garbagereporting.messaging.event.*",
                "java.lang.*",
                "java.time.*",
                "java.util.*"
        );
        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            SimpleMessageConverter rabbitMessageConverter
    ) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(rabbitMessageConverter);
        return rabbitTemplate;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            SimpleMessageConverter rabbitMessageConverter
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(rabbitMessageConverter);
        return factory;
    }

    @Bean
    public Queue reportProcessingQueue(ApplicationProperties properties) {
        return new Queue(
                properties.getMessaging().getReportProcessingQueue(),
                true,
                false,
                false,
                Map.of(
                        "x-dead-letter-exchange", properties.getMessaging().getReportProcessingDlx(),
                        "x-dead-letter-routing-key", properties.getMessaging().getReportProcessingDlqRoutingKey()
                )
        );
    }

    @Bean
    public DirectExchange reportProcessingExchange(ApplicationProperties properties) {
        return new DirectExchange(properties.getMessaging().getReportProcessingExchange(), true, false);
    }

    @Bean
    public Binding reportProcessingBinding(
            Queue reportProcessingQueue,
            DirectExchange reportProcessingExchange,
            ApplicationProperties properties
    ) {
        return BindingBuilder.bind(reportProcessingQueue)
                .to(reportProcessingExchange)
                .with(properties.getMessaging().getReportProcessingRoutingKey());
    }

    @Bean
    public DirectExchange reportProcessingDeadLetterExchange(ApplicationProperties properties) {
        return new DirectExchange(properties.getMessaging().getReportProcessingDlx(), true, false);
    }

    @Bean
    public Queue reportProcessingDeadLetterQueue(ApplicationProperties properties) {
        return new Queue(properties.getMessaging().getReportProcessingDlq(), true);
    }

    @Bean
    public Binding reportProcessingDeadLetterBinding(
            Queue reportProcessingDeadLetterQueue,
            DirectExchange reportProcessingDeadLetterExchange,
            ApplicationProperties properties
    ) {
        return BindingBuilder.bind(reportProcessingDeadLetterQueue)
                .to(reportProcessingDeadLetterExchange)
                .with(properties.getMessaging().getReportProcessingDlqRoutingKey());
    }
}
