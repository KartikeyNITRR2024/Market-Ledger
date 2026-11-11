package com.checkin.ingestion.stock;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface IngestionErrorService {

    Page<IngestionErrorResponseDto> getErrorsByTaskId(UUID taskId, Pageable pageable);
}