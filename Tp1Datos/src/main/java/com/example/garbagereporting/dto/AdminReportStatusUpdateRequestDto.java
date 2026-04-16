package com.example.garbagereporting.dto;

import com.example.garbagereporting.model.AdminReportStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminReportStatusUpdateRequestDto {
    @NotNull
    private AdminReportStatus status;
}
