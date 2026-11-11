package com.checkin.ingestion.settings;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemSettingResponseDto {

    private String id;
    private Integer value;
    private UUID updatedBy;
    private LocalDateTime updatedAt;
}