package com.example.garbagereporting.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.garbagereporting.dto.AdminReportResponseDto;
import com.example.garbagereporting.dto.AuthenticatedReportUserDto;
import com.example.garbagereporting.dto.ReportCreateRequestDto;
import com.example.garbagereporting.dto.ReportSubmissionResponseDto;
import com.example.garbagereporting.exception.BusinessValidationException;
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
import org.springframework.mock.web.MockMultipartFile;

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

    @Test
    void rejectsImmediateDuplicateSubmissionForSameImageAndCoordinates() {
        ReportCreateRequestDto request = buildRequest(
                new MockMultipartFile("image", "same.jpg", "image/jpeg", new byte[]{1, 2, 3, 4}),
                -31.420083,
                -64.188776
        );
        AuthenticatedReportUserDto user = buildUser();

        when(imageStorageService.store(any())).thenReturn("uploads/generated.jpg");
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(reportProcessingProducer).publish(any());

        ReportSubmissionResponseDto firstResponse = reportService.submitReportForProcessing(request, user);

        assertThat(firstResponse.getStatus()).isEqualTo("QUEUED");
        assertThatThrownBy(() -> reportService.submitReportForProcessing(request, user))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessage("Se detecto un envio duplicado inmediato del mismo reporte.");

        verify(imageStorageService, times(1)).store(any());
        verify(reportProcessingProducer, times(1)).publish(any());
    }

    @Test
    void allowsRetryWhenFirstSubmissionFailsBeforeAccepting() {
        ReportCreateRequestDto request = buildRequest(
                new MockMultipartFile("image", "retry.jpg", "image/jpeg", new byte[]{5, 6, 7, 8}),
                -31.421000,
                -64.190000
        );
        AuthenticatedReportUserDto user = buildUser();

        when(imageStorageService.store(any()))
                .thenThrow(new RuntimeException("disk error"))
                .thenReturn("uploads/retry.jpg");
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(reportProcessingProducer).publish(any());

        assertThatThrownBy(() -> reportService.submitReportForProcessing(request, user))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("disk error");

        ReportSubmissionResponseDto retryResponse = reportService.submitReportForProcessing(request, user);

        assertThat(retryResponse.getStatus()).isEqualTo("QUEUED");
        verify(imageStorageService, times(2)).store(any());
        verify(reportRepository, times(1)).save(any(Report.class));
        verify(reportProcessingProducer, times(1)).publish(any());
    }

    private ReportCreateRequestDto buildRequest(MockMultipartFile image, double lat, double lng) {
        ReportCreateRequestDto request = new ReportCreateRequestDto();
        request.setImage(image);
        request.setLat(lat);
        request.setLng(lng);
        return request;
    }

    private AuthenticatedReportUserDto buildUser() {
        return AuthenticatedReportUserDto.builder()
                .userId("user-1")
                .email("user1@test.local")
                .displayName("User One")
                .build();
    }
}
