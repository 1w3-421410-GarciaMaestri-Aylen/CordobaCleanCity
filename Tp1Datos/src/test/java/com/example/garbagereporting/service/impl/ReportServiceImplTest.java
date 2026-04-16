package com.example.garbagereporting.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.garbagereporting.dto.AdminReportResponseDto;
import com.example.garbagereporting.mapper.ReportMapper;
import com.example.garbagereporting.model.Report;
import com.example.garbagereporting.model.ReportStatus;
import com.example.garbagereporting.repository.ReportRepository;
import com.example.garbagereporting.service.ImageStorageService;
import com.example.garbagereporting.service.RouteService;
import com.example.garbagereporting.messaging.producer.ReportProcessingProducer;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock
    private ReportRepository reportRepository;
    @Mock
    private ReportMapper reportMapper;
    @Mock
    private ImageStorageService imageStorageService;
    @Mock
    private ReportProcessingProducer reportProcessingProducer;
    @Mock
    private RouteService routeService;

    @InjectMocks
    private ReportServiceImpl reportService;

    @Test
    void adminListingIncludesPendingAndRejectedReportsForTraceability() {
        Report pending = Report.builder()
                .id("pending-1")
                .status(ReportStatus.PENDING)
                .createdAt(Instant.now())
                .build();
        Report invalid = Report.builder()
                .id("invalid-1")
                .status(ReportStatus.PROCESSED_INVALID)
                .createdAt(Instant.now().minusSeconds(30))
                .build();
        AdminReportResponseDto pendingDto = AdminReportResponseDto.builder().id("pending-1").build();
        AdminReportResponseDto invalidDto = AdminReportResponseDto.builder().id("invalid-1").build();

        when(reportRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(pending, invalid));
        when(reportMapper.toAdminResponseDto(pending)).thenReturn(pendingDto);
        when(reportMapper.toAdminResponseDto(invalid)).thenReturn(invalidDto);

        List<AdminReportResponseDto> reports = reportService.getReportsForAdmin();

        verify(reportRepository).findAllByOrderByCreatedAtDesc();
        assertThat(reports).containsExactly(pendingDto, invalidDto);
    }
}
