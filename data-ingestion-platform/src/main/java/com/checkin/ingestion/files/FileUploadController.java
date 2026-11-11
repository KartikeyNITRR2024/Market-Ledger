package com.checkin.ingestion.files;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.checkin.ingestion.common.util.JwtService;
import org.springframework.data.domain.Sort;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/files")
@RequiredArgsConstructor
public class FileUploadController {
    
    private final FileUploadService fileUploadService;
    private final JwtService jwtService;
    private final TaskService taskService;
    
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/upload")
    public ResponseEntity<UploadTaskResponseDto> upload(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("Authorization") String authHeader) throws IOException {
        
        String token = authHeader.substring(7);
        String uuidString = jwtService.extractClaim(token, claims -> claims.get("uuid", String.class));
        UUID uploaderUuid = UUID.fromString(uuidString);
        return ResponseEntity.accepted().body(fileUploadService.accept(file, uploaderUuid));
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<UploadTaskResponseDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(taskService.get(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Page<UploadTaskResponseDto>> list(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(taskService.list(pageable));
    }
}