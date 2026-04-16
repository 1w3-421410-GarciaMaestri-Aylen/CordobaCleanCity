package com.example.garbagereporting.mapper;

import com.example.garbagereporting.config.ApplicationProperties;
import com.example.garbagereporting.dto.AdminReportResponseDto;
import com.example.garbagereporting.dto.LocationResponseDto;
import com.example.garbagereporting.dto.ReportResponseDto;
import com.example.garbagereporting.model.AdminReportStatus;
import com.example.garbagereporting.model.Location;
import com.example.garbagereporting.model.Report;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReportMapper {

    private final ApplicationProperties properties;

    public ReportResponseDto toResponseDto(Report report) {
        Location location = resolveLocation(report);
        String userLabel = resolveUserLabel(report);

        return ReportResponseDto.builder()
                .id(report.getId())
                .userId(report.getUserId())
                .userEmail(report.getUserEmail())
                .user(userLabel)
                .imageUrl(toPublicImageUrl(report.getImageUrl()))
                .location(LocationResponseDto.builder()
                        .lat(location.getLat())
                        .lng(location.getLng())
                        .build())
                .createdAt(report.getCreatedAt())
                .status(report.getStatus())
                .classificationResult(report.getClassificationResult())
                .isTrash(report.isTrash())
                .build();
    }

    public AdminReportResponseDto toAdminResponseDto(Report report) {
        Location location = resolveLocation(report);
        String userLabel = resolveUserLabel(report);

        return AdminReportResponseDto.builder()
                .id(report.getId())
                .requestId(report.getRequestId())
                .userId(report.getUserId())
                .userEmail(report.getUserEmail())
                .user(userLabel)
                .imageUrl(toPublicImageUrl(report.getImageUrl()))
                .location(LocationResponseDto.builder()
                        .lat(location.getLat())
                        .lng(location.getLng())
                        .build())
                .createdAt(report.getCreatedAt())
                .status(report.getStatus())
                .adminStatus(report.getAdminStatus() == null ? AdminReportStatus.PENDING : report.getAdminStatus())
                .adminStatusUpdatedAt(report.getAdminStatusUpdatedAt())
                .classificationResult(report.getClassificationResult())
                .isTrash(report.isTrash())
                .build();
    }

    private Location resolveLocation(Report report) {
        Location location = report.getLocation() == null ? new Location(0.0, 0.0) : report.getLocation();
        return location;
    }

    private String resolveUserLabel(Report report) {
        return report.getUser() != null && !report.getUser().isBlank()
                ? report.getUser()
                : report.getUserEmail();
    }

    private String toPublicImageUrl(String storedImagePath) {
        if (storedImagePath == null || storedImagePath.isBlank()) {
            return storedImagePath;
        }

        Path fileName = Paths.get(storedImagePath).getFileName();
        if (fileName == null) {
            return storedImagePath;
        }

        String imageDirName = Paths.get(properties.getStorage().getImageDir()).getFileName().toString().replace("\\", "/");
        return "/" + imageDirName + "/" + fileName.toString();
    }
}
