package com.checkin.ingestion.stock;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IngestionErrorServiceImpl implements IngestionErrorService {

    private final IngestionErrorRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Page<IngestionErrorResponseDto> getErrorsByTaskId(UUID taskId, Pageable pageable) {
        return repository.findByTaskId(taskId, pageable)
                .map(IngestionErrorResponseDto::fromEntity);
    }
}