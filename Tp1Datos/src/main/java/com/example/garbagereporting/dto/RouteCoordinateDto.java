package com.example.garbagereporting.dto;

import java.io.Serial;
import java.io.Serializable;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RouteCoordinateDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    Double lat;
    Double lng;
}
