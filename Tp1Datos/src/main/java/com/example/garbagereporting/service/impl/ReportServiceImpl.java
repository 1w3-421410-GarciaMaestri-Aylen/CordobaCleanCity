package com.example.garbagereporting.service.impl;

import com.example.garbagereporting.cache.CacheNames;
import com.example.garbagereporting.dto.AdminReportResponseDto;
import com.example.garbagereporting.dto.AuthenticatedReportUserDto;
import com.example.garbagereporting.dto.ReportCreateRequestDto;
import com.example.garbagereporting.dto.ReportResponseDto;
import com.example.garbagereporting.dto.ReportSubmissionResponseDto;
import com.example.garbagereporting.exception.BusinessValidationException;
import com.example.garbagereporting.exception.ResourceNotFoundException;
import com.example.garbagereporting.mapper.ReportMapper;
import com.example.garbagereporting.model.AdminReportStatus;
import com.example.garbagereporting.messaging.event.ReportProcessingEvent;
import com.example.garbagereporting.messaging.producer.ReportProcessingProducer;
import com.example.garbagereporting.model.Location;
import com.example.garbagereporting.model.Report;
import com.example.garbagereporting.model.ReportStatus;
import com.example.garbagereporting.repository.ReportRepository;
import com.example.garbagereporting.service.ImageStorageService;
import com.example.garbagereporting.service.ReportService;
import com.example.garbagereporting.service.RouteService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private static final List<ReportStatus> VISIBLE_STATUSES = List.of(
            ReportStatus.PROCESSED_VALID,
            ReportStatus.CONFIRMED,
            ReportStatus.PROCESSED
    );

    private final ReportRepository reportRepository;
    private final ReportMapper reportMapper;
    private final ImageStorageService imageStorageService;
    private final ReportProcessingProducer reportProcessingProducer;
    private final RouteService routeService;

    @Override
    public ReportSubmissionResponseDto submitReportForProcessing(
            ReportCreateRequestDto request,
            AuthenticatedReportUserDto authenticatedUser
    ) {
        if (request.getImage() == null || request.getImage().isEmpty()) {
            throw new BusinessValidationException("image must not be empty");
        }
        if (authenticatedUser == null || authenticatedUser.getUserId() == null || authenticatedUser.getEmail() == null) {
            throw new BusinessValidationException("authenticated user context is required");
        }

        String imageUrl = imageStorageService.store(request.getImage());
        String requestId = UUID.randomUUID().toString();
        Instant now = Instant.now();

        Report pendingReport = Report.builder()
                .requestId(requestId)
                .userId(authenticatedUser.getUserId())
                .userEmail(authenticatedUser.getEmail())
                .user(authenticatedUser.getDisplayName())
                .imageUrl(imageUrl)
                .location(Location.builder()
                        .lat(request.getLat())
                        .lng(request.getLng())
                        .build())
                .createdAt(now)
                .status(ReportStatus.PENDING)
                .adminStatus(AdminReportStatus.PENDING)
                .adminStatusUpdatedAt(now)
                .classificationResult("Procesando imagen")
                .isTrash(false)
                .build();
        reportRepository.save(pendingReport);
        evictTodayReportsCache();
        log.info(
                "Report queued requestId={} userId={} lat={} lng={} imageUrl={}",
                requestId,
                authenticatedUser.getUserId(),
                request.getLat(),
                request.getLng(),
                imageUrl
        );

        reportProcessingProducer.publish(ReportProcessingEvent.builder()
                .requestId(requestId)
                .userId(authenticatedUser.getUserId())
                .userEmail(authenticatedUser.getEmail())
                .userDisplayName(authenticatedUser.getDisplayName())
                .imageUrl(imageUrl)
                .originalFilename(request.getImage().getOriginalFilename())
                .lat(request.getLat())
                .lng(request.getLng())
                .submittedAt(now)
                .build());

        return ReportSubmissionResponseDto.builder()
                .requestId(requestId)
                .status("QUEUED")
                .message("Imagen recibida y registrada para procesamiento asincrono")
                .imageUrl(imageUrl)
                .build();
    }

    @Override
    public List<ReportResponseDto> getAllReports() {
        return reportRepository.findByStatusInOrderByCreatedAtDesc(VISIBLE_STATUSES)
                .stream()
                .map(reportMapper::toResponseDto)
                .toList();
    }

    @Override
    public List<ReportResponseDto> getTodayReports() {
        ZoneId zoneId = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zoneId);

        Instant from = today.atStartOfDay(zoneId).toInstant();
        Instant to = today.plusDays(1).atStartOfDay(zoneId).toInstant();

        return reportRepository.findByCreatedAtRangeAndStatusInOrderByCreatedAtDesc(from, to, VISIBLE_STATUSES)
                .stream()
                .map(reportMapper::toResponseDto)
                .toList();
    }

    @Override
    public List<ReportResponseDto> getReportsByUserId(String userId) {
        return reportRepository.findByUserIdAndStatusInOrderByCreatedAtDesc(userId, VISIBLE_STATUSES)
                .stream()
                .map(reportMapper::toResponseDto)
                .toList();
    }

    @Override
    public List<ReportResponseDto> getTodayReportsByUserId(String userId) {
        ZoneId zoneId = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zoneId);

        Instant from = today.atStartOfDay(zoneId).toInstant();
        Instant to = today.plusDays(1).atStartOfDay(zoneId).toInstant();

        return reportRepository.findByUserIdAndCreatedAtRangeAndStatusInOrderByCreatedAtDesc(
                        userId,
                        from,
                        to,
                        VISIBLE_STATUSES
                )
                .stream()
                .map(reportMapper::toResponseDto)
                .toList();
    }

    @Override
    public List<AdminReportResponseDto> getReportsForAdmin() {
        return reportRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(reportMapper::toAdminResponseDto)
                .toList();
    }

    @Override
    public AdminReportResponseDto updateAdminStatus(String reportId, AdminReportStatus status) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        if (!VISIBLE_STATUSES.contains(report.getStatus())) {
            throw new BusinessValidationException("Only approved reports can be updated from admin");
        }

        report.setAdminStatus(status);
        report.setAdminStatusUpdatedAt(Instant.now());
        Report savedReport = reportRepository.save(report);
        routeService.evictOptimalRoutesCache();
        return reportMapper.toAdminResponseDto(savedReport);
    }

    @Override
    @CacheEvict(cacheNames = CacheNames.REPORTS_TODAY, allEntries = true)
    public void evictTodayReportsCache() {
    }
}
