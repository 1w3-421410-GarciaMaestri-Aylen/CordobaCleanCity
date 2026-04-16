package com.example.garbagereporting.controller;

import com.example.garbagereporting.dto.ReportCreateRequestDto;
import com.example.garbagereporting.dto.ReportResponseDto;
import com.example.garbagereporting.dto.ReportSubmissionResponseDto;
import com.example.garbagereporting.dto.AuthenticatedReportUserDto;
import com.example.garbagereporting.exception.ForbiddenException;
import com.example.garbagereporting.service.AuthService;
import com.example.garbagereporting.service.ReportService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private final AuthService authService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<ReportSubmissionResponseDto> createReport(
            @Valid @ModelAttribute ReportCreateRequestDto request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        var currentUser = authService.getCurrentUser(jwt.getSubject());
        if (!currentUser.isEmailVerified()) {
            throw new ForbiddenException("Email not verified");
        }

        AuthenticatedReportUserDto authenticatedUser = AuthenticatedReportUserDto.builder()
                .userId(resolveUserId(jwt, currentUser.getId()))
                .email(currentUser.getEmail())
                .displayName(buildDisplayName(currentUser.getFirstName(), currentUser.getLastName(), currentUser.getEmail()))
                .build();

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
                reportService.submitReportForProcessing(request, authenticatedUser)
        );
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReportResponseDto>> getAllReports() {
        return ResponseEntity.ok(reportService.getAllReports());
    }

    @GetMapping("/today")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReportResponseDto>> getTodayReports() {
        return ResponseEntity.ok(reportService.getTodayReports());
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<List<ReportResponseDto>> getMyReports(@AuthenticationPrincipal Jwt jwt) {
        String userId = resolveUserId(jwt, null);
        return ResponseEntity.ok(reportService.getReportsByUserId(userId));
    }

    @GetMapping("/me/today")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<List<ReportResponseDto>> getMyTodayReports(@AuthenticationPrincipal Jwt jwt) {
        String userId = resolveUserId(jwt, null);
        return ResponseEntity.ok(reportService.getTodayReportsByUserId(userId));
    }

    private String buildDisplayName(String firstName, String lastName, String email) {
        String normalizedFirst = firstName == null ? "" : firstName.trim();
        String normalizedLast = lastName == null ? "" : lastName.trim();
        String displayName = (normalizedFirst + " " + normalizedLast).trim();
        return displayName.isBlank() ? email : displayName;
    }

    private String resolveUserId(Jwt jwt, String fallbackUserId) {
        String claimUserId = jwt.getClaimAsString("uid");
        if (StringUtils.hasText(claimUserId)) {
            return claimUserId.trim();
        }
        if (StringUtils.hasText(fallbackUserId)) {
            return fallbackUserId.trim();
        }
        String userId = authService.getCurrentUser(jwt.getSubject()).getId();
        if (!StringUtils.hasText(userId)) {
            throw new ForbiddenException("Invalid authenticated user");
        }
        return userId.trim();
    }
}
