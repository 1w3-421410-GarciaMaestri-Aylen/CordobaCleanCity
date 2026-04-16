package com.example.garbagereporting.dto;

import java.io.Serial;
import java.io.Serializable;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RouteMetadataDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    String algorithm;
    String scope;
    Integer sourceReportCount;
    String routingProvider;
    String routingStatus;
    String routingMessage;
}
