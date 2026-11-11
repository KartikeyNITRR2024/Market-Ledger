package com.checkin.ingestion.files;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.checkin.ingestion.worker.IngestionWorker;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileUploadServiceImpl implements FileUploadService {

    private final TaskService taskService;
    private final IngestionWorker worker;

    @Value("${ingestion.upload-dir:/data/uploads}")
    private String uploadDirName;

    private Path uploadDir;

    @PostConstruct
    void init() throws IOException {
        uploadDir = Path.of(uploadDirName);
        Files.createDirectories(uploadDir);
    }

    @Override
    public UploadTaskResponseDto accept(MultipartFile file, UUID uploadedBy) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }
        String original = file.getOriginalFilename() == null ? "upload.csv" : file.getOriginalFilename();
        if (!original.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only .csv files are accepted");
        }

        UUID id = UUID.randomUUID();
        UploadTask uploadTask = new UploadTask();
        // Our own file name: the client-supplied name is never used as a path (no path traversal).
        Path target = uploadDir.resolve(id + ".csv");

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING); // buffered copy, constant memory
        } catch (IOException e) {
            Files.deleteIfExists(target);
            throw e;
        }

        try {
        	uploadTask = taskService.createPending(id, displayName(original), target.toString(), uploadedBy);
        } catch (RuntimeException e) {
            Files.deleteIfExists(target);
            throw e;
        }

        try {
            worker.process(id);
        } catch (TaskRejectedException e) {
            taskService.markFailed(id, "Server busy: ingestion queue is full");
            Files.deleteIfExists(target);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Ingestion queue is full, try again shortly");
        }
        
        return UploadTaskResponseDto.builder()
        		.id(uploadTask.getId())
        		.fileName(uploadTask.getFileName())
        		.status(uploadTask.getStatus())
        		.uploadedBy(uploadTask.getUploadedBy())
        		.createdAt(uploadTask.getCreatedAt())
        		.build();
    }

    private String displayName(String original) {
        String cleaned = original.replaceAll("[^A-Za-z0-9._ -]", "_");
        return cleaned.length() > 255 ? cleaned.substring(0, 255) : cleaned;
    }
}
