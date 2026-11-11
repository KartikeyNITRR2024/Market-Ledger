package com.checkin.ingestion.stock;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IngestionErrorResponseDto {

    private Long id;
    private UUID taskId;
    private long rowNum;
    private String rawRow;
    private String reason;

    public static IngestionErrorResponseDto fromEntity(IngestionError error) {
        if (error == null) {
            return null;
        }

        return IngestionErrorResponseDto.builder()
                .id(error.getId())
                .taskId(error.getTaskId())
                .rowNum(error.getRowNum())
                .rawRow(error.getRawRow())
                .reason(error.getReason())
                .build();
    }
}