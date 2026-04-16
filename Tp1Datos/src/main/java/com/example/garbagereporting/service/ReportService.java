package com.example.garbagereporting.service;

import com.example.garbagereporting.dto.AdminReportResponseDto;
import com.example.garbagereporting.model.AdminReportStatus;
import com.example.garbagereporting.dto.ReportCreateRequestDto;
import com.example.garbagereporting.dto.ReportSubmissionResponseDto;
import com.example.garbagereporting.dto.ReportResponseDto;
import com.example.garbagereporting.dto.AuthenticatedReportUserDto;
import java.util.List;

public interface ReportService {

    ReportSubmissionResponseDto submitReportForProcessing(
            ReportCreateRequestDto request,
            AuthenticatedReportUserDto authenticatedUser
    );

    List<ReportResponseDto> getAllReports();

    List<ReportResponseDto> getTodayReports();

    List<ReportResponseDto> getReportsByUserId(String userId);

    List<ReportResponseDto> getTodayReportsByUserId(String userId);

    List<AdminReportResponseDto> getReportsForAdmin();

    AdminReportResponseDto updateAdminStatus(String reportId, AdminReportStatus status);

    void evictTodayReportsCache();
}
