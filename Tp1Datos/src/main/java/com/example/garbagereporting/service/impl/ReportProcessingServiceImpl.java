package com.example.garbagereporting.service.impl;

import com.example.garbagereporting.dto.VisionClassificationResultDto;
import com.example.garbagereporting.messaging.event.ReportProcessingEvent;
import com.example.garbagereporting.model.AdminReportStatus;
import com.example.garbagereporting.model.Location;
import com.example.garbagereporting.model.Report;
import com.example.garbagereporting.model.ReportStatus;
import com.example.garbagereporting.repository.ReportRepository;
import com.example.garbagereporting.service.ImageStorageService;
import com.example.garbagereporting.service.ReportProcessingService;
import com.example.garbagereporting.service.ReportService;
import com.example.garbagereporting.service.RouteService;
import com.example.garbagereporting.service.VisionClassificationService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportProcessingServiceImpl implements ReportProcessingService {

    private final VisionClassificationService visionClassificationService;
    private final ReportRepository reportRepository;
    private final ReportService reportService;
    private final RouteService routeService;
    private final ImageStorageService imageStorageService;

    @Override
    public void process(ReportProcessingEvent event) {
        VisionClassificationResultDto classification = visionClassificationService.classify(
                event.getImageUrl(),
                event.getOriginalFilename()
        );
        log.info(
                "Classification completed requestId={} originalFilename={} isTrash={} confidence={} result={}",
                event.getRequestId(),
                event.getOriginalFilename(),
                classification.isTrash(),
                classification.getConfidence(),
                classification.getClassificationResult()
        );
        Report report = reportRepository.findByRequestId(event.getRequestId()).orElse(null);

        if (!classification.isTrash()) {
            log.info("Discarding requestId={} because no trash was detected", event.getRequestId());
            if (report != null) {
                report.setStatus(ReportStatus.PROCESSED_INVALID);
                report.setClassificationResult(classification.getClassificationResult());
                report.setTrash(false);
                reportRepository.save(report);
                reportService.evictTodayReportsCache();
                routeService.evictOptimalRoutesCache();
            }
            imageStorageService.deleteIfExists(event.getImageUrl());
            return;
        }

        if (report == null) {
            Instant now = Instant.now();
            report = Report.builder()
                    .requestId(event.getRequestId())
                    .userId(event.getUserId())
                    .userEmail(event.getUserEmail())
                    .user(event.getUserDisplayName())
                    .imageUrl(event.getImageUrl())
                    .location(Location.builder()
                            .lat(event.getLat())
                            .lng(event.getLng())
                            .build())
                    .createdAt(now)
                    .adminStatus(AdminReportStatus.PENDING)
                    .adminStatusUpdatedAt(now)
                    .build();
        }

        if (report.getAdminStatus() == null) {
            report.setAdminStatus(AdminReportStatus.PENDING);
        }
        if (report.getAdminStatusUpdatedAt() == null) {
            report.setAdminStatusUpdatedAt(Instant.now());
        }
        report.setStatus(ReportStatus.PROCESSED_VALID);
        report.setClassificationResult(classification.getClassificationResult());
        report.setTrash(true);

        reportRepository.save(report);
        reportService.evictTodayReportsCache();
        routeService.evictOptimalRoutesCache();
        log.info("Async report persisted for requestId={}", event.getRequestId());
    }
}
