package com.example.garbagereporting.service;

import com.example.garbagereporting.messaging.event.ReportProcessingEvent;

public interface ReportProcessingService {

    void process(ReportProcessingEvent event);
}
