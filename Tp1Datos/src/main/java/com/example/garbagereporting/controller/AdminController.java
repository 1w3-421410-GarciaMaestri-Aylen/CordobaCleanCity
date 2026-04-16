package com.example.garbagereporting.controller;

import com.example.garbagereporting.dto.AdminReportResponseDto;
import com.example.garbagereporting.dto.AdminReportStatusUpdateRequestDto;
import com.example.garbagereporting.dto.AuthUserResponseDto;
import com.example.garbagereporting.service.AuthService;
import com.example.garbagereporting.service.ReportService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {

    private final AuthService authService;
    private final ReportService reportService;

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AuthUserResponseDto>> getAllUsers() {
        return ResponseEntity.ok(authService.getAllUsers());
    }

    @GetMapping("/reports")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AdminReportResponseDto>> getReports() {
        return ResponseEntity.ok(reportService.getReportsForAdmin());
    }

    @PatchMapping("/reports/{reportId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminReportResponseDto> updateReportStatus(
            @PathVariable String reportId,
            @Valid @RequestBody AdminReportStatusUpdateRequestDto request
    ) {
        return ResponseEntity.ok(reportService.updateAdminStatus(reportId, request.getStatus()));
    }
}
