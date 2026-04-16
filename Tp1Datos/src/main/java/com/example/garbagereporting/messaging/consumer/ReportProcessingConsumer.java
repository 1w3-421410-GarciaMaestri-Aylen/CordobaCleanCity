package com.example.garbagereporting.messaging.consumer;

import com.example.garbagereporting.messaging.event.ReportProcessingEvent;
import com.example.garbagereporting.service.ReportProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportProcessingConsumer {

    private final ReportProcessingService reportProcessingService;

    @RabbitListener(queues = "${app.messaging.report-processing-queue}")
    public void consume(ReportProcessingEvent event) {
        log.info("Processing async report requestId={}", event.getRequestId());
        reportProcessingService.process(event);
    }
}
