package com.checkin.ingestion.files;

import java.time.LocalDateTime;
import java.util.UUID;

import com.checkin.ingestion.user.Role;
import com.checkin.ingestion.user.UserResponseDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadTaskResponseDto {
	private UUID id;
    private String fileName;
    private UUID uploadedBy;
    private TaskStatus status;
    private Long totalRows;
    private Long processedRows;
    private Long errorCount;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
