package com.checkin.ingestion.files;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.checkin.ingestion.user.User;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final UploadTaskRepository repository;
    private final JdbcTemplate jdbc;

    public UploadTask createPending(UUID id, String fileName, String filePath, UUID uploadedBy) {
        
        UploadTask task = UploadTask.builder()
        		.id(id)
        		.fileName(fileName)
        		.filePath(filePath)
        		.uploadedBy(uploadedBy)
        		.status(TaskStatus.PENDING)
        		.build();
        
        return repository.save(task);
    }

    public boolean claim(UUID id) {
        return jdbc.update(
                "UPDATE upload_tasks SET status = 'PROCESSING', started_at = now() "
                        + "WHERE id = ? AND status = 'PENDING'", id) == 1;
    }

    public void setTotalRows(UUID id, long total) {
        jdbc.update("UPDATE upload_tasks SET total_rows = ? WHERE id = ?", total, id);
    }

    public void markCompleted(UUID id) {
        jdbc.update("UPDATE upload_tasks SET status = 'COMPLETED', finished_at = now() WHERE id = ?", id);
    }

    public void markFailed(UUID id, String message) {
        jdbc.update("UPDATE upload_tasks SET status = 'FAILED', error_message = ?, finished_at = now() WHERE id = ?",
                message, id);
    }

    public UploadTaskResponseDto get(UUID id) {
        return repository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found: " + id));
    }

    public Page<UploadTaskResponseDto> list(Pageable pageable) {
        return repository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toDto);
    }

    private UploadTaskResponseDto toDto(UploadTask task) {
        return UploadTaskResponseDto.builder()
                .id(task.getId())
                .fileName(task.getFileName())
                .uploadedBy(task.getUploadedBy())
                .status(task.getStatus())
                .totalRows(task.getTotalRows())
                .processedRows(task.getProcessedRows())
                .errorCount(task.getErrorCount())
                .errorMessage(task.getErrorMessage())
                .createdAt(task.getCreatedAt())
                .startedAt(task.getStartedAt())
                .finishedAt(task.getFinishedAt())
                .build();
    }
}
