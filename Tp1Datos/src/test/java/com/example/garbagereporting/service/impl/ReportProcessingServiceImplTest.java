package com.example.garbagereporting.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.garbagereporting.dto.VisionClassificationResultDto;
import com.example.garbagereporting.messaging.event.ReportProcessingEvent;
import com.example.garbagereporting.model.Report;
import com.example.garbagereporting.model.ReportStatus;
import com.example.garbagereporting.repository.ReportRepository;
import com.example.garbagereporting.service.ImageStorageService;
import com.example.garbagereporting.service.ReportService;
import com.example.garbagereporting.service.RouteService;
import com.example.garbagereporting.service.VisionClassificationService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportProcessingServiceImplTest {

    @Mock
    private VisionClassificationService visionClassificationService;
    @Mock
    private ReportRepository reportRepository;
    @Mock
    private ReportService reportService;
    @Mock
    private RouteService routeService;
    @Mock
    private ImageStorageService imageStorageService;

    @InjectMocks
    private ReportProcessingServiceImpl reportProcessingService;

    @Test
    void marksReportAsProcessedInvalidWhenVisionRejectsImage() {
        Report report = Report.builder().requestId("req-1").status(ReportStatus.PENDING).build();
        ReportProcessingEvent event = baseEvent("parque_limpio.jpg");

        when(visionClassificationService.classify("uploads/random.jpg", "parque_limpio.jpg"))
                .thenReturn(VisionClassificationResultDto.builder()
                        .isTrash(false)
                        .classificationResult("No trash detected")
                        .confidence(0.91)
                        .build());
        when(reportRepository.findByRequestId("req-1")).thenReturn(Optional.of(report));

        reportProcessingService.process(event);

        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).save(captor.capture());
        verify(imageStorageService).deleteIfExists("uploads/random.jpg");
        verify(reportService).evictTodayReportsCache();
        verify(routeService).evictOptimalRoutesCache();
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getStatus()).isEqualTo(ReportStatus.PROCESSED_INVALID);
        org.assertj.core.api.Assertions.assertThat(captor.getValue().isTrash()).isFalse();
    }

    @Test
    void marksReportAsProcessedValidWhenVisionAcceptsImage() {
        Report report = Report.builder().requestId("req-1").status(ReportStatus.PENDING).build();
        ReportProcessingEvent event = baseEvent("basura_esquina.jpg");

        when(visionClassificationService.classify("uploads/random.jpg", "basura_esquina.jpg"))
                .thenReturn(VisionClassificationResultDto.builder()
                        .isTrash(true)
                        .classificationResult("Trash detected")
                        .confidence(0.88)
                        .build());
        when(reportRepository.findByRequestId("req-1")).thenReturn(Optional.of(report));

        reportProcessingService.process(event);

        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).save(captor.capture());
        verify(imageStorageService, never()).deleteIfExists(any());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getStatus()).isEqualTo(ReportStatus.PROCESSED_VALID);
        org.assertj.core.api.Assertions.assertThat(captor.getValue().isTrash()).isTrue();
    }

    private ReportProcessingEvent baseEvent(String originalFilename) {
        return ReportProcessingEvent.builder()
                .requestId("req-1")
                .userId("user-1")
                .userEmail("user@test.local")
                .userDisplayName("User Test")
                .imageUrl("uploads/random.jpg")
                .originalFilename(originalFilename)
                .lat(-31.4)
                .lng(-64.18)
                .build();
    }
}
