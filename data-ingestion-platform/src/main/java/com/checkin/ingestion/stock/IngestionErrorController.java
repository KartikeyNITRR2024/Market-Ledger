package com.checkin.ingestion.stock;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/ingestion-errors")
@RequiredArgsConstructor
public class IngestionErrorController {

    private final IngestionErrorService ingestionErrorService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<Page<IngestionErrorResponseDto>> getErrorsByTaskId(
            @PathVariable UUID taskId,
            @PageableDefault(size = 20, sort = "rowNum", direction = Sort.Direction.ASC) Pageable pageable) {

        Page<IngestionErrorResponseDto> errors = ingestionErrorService.getErrorsByTaskId(taskId, pageable);
        return ResponseEntity.ok(errors);
    }
}