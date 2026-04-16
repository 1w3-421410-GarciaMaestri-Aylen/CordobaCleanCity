package com.example.garbagereporting.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.garbagereporting.exception.BusinessValidationException;
import com.example.garbagereporting.dto.RouteCoordinateDto;
import com.example.garbagereporting.dto.RouteScope;
import com.example.garbagereporting.model.AdminReportStatus;
import com.example.garbagereporting.model.Location;
import com.example.garbagereporting.model.Report;
import com.example.garbagereporting.model.ReportStatus;
import com.example.garbagereporting.repository.ReportRepository;
import com.example.garbagereporting.service.RoadRoutingService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RouteServiceImplTest {

    @Mock
    private ReportRepository reportRepository;
    @Mock
    private RoadRoutingService roadRoutingService;

    @InjectMocks
    private RouteServiceImpl routeService;

    @Test
    void activeRouteUsesManualStartAndOnlyPendingOperationalReports() {
        when(reportRepository.findByStatusInOrderByCreatedAtDesc(anyList()))
                .thenReturn(List.of(
                        Report.builder()
                        .id("rep-1")
                        .createdAt(Instant.now())
                        .adminStatus(AdminReportStatus.PENDING)
                        .status(ReportStatus.PROCESSED_VALID)
                        .location(Location.builder().lat(-31.4).lng(-64.18).build())
                        .build(),
                        Report.builder()
                                .id("rep-2")
                                .createdAt(Instant.now())
                                .adminStatus(AdminReportStatus.RESOLVED)
                                .status(ReportStatus.CONFIRMED)
                                .location(Location.builder().lat(-31.41).lng(-64.19).build())
                                .build()
                ));
        when(roadRoutingService.buildRoute(anyList()))
                .thenReturn(new RoadRoutingService.RoadRoutingResult(
                        List.of(
                                RouteCoordinateDto.builder().lat(-31.42).lng(-64.20).build(),
                                RouteCoordinateDto.builder().lat(-31.4).lng(-64.18).build()
                        ),
                        1.8,
                        "mock",
                        "success",
                        null
                ));

        routeService.getOptimalRoute(RouteScope.ACTIVE, -31.42, -64.20);

        ArgumentCaptor<List<ReportStatus>> statusesCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<RouteCoordinateDto>> coordinatesCaptor = ArgumentCaptor.forClass(List.class);
        verify(reportRepository).findByStatusInOrderByCreatedAtDesc(statusesCaptor.capture());
        verify(roadRoutingService).buildRoute(coordinatesCaptor.capture());
        assertThat(statusesCaptor.getValue())
                .contains(ReportStatus.PROCESSED_VALID, ReportStatus.CONFIRMED, ReportStatus.PROCESSED)
                .doesNotContain(ReportStatus.PENDING, ReportStatus.PROCESSED_INVALID, ReportStatus.REJECTED);
        assertThat(coordinatesCaptor.getValue())
                .hasSize(2)
                .extracting(RouteCoordinateDto::getLat, RouteCoordinateDto::getLng)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(-31.42, -64.20),
                        org.assertj.core.groups.Tuple.tuple(-31.4, -64.18)
                );
    }

    @Test
    void rejectsStartOutsideCordobaCapital() {
        assertThatThrownBy(() -> routeService.getOptimalRoute(RouteScope.ACTIVE, -32.0, -64.20))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessage("La ubicacion inicial debe estar dentro de Cordoba Capital.");
    }
}
