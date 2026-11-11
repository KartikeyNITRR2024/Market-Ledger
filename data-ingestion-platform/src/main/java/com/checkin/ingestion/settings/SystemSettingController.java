package com.checkin.ingestion.settings;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/system-settings")
@RequiredArgsConstructor
public class SystemSettingController {

    private final SystemSettingService service;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<SystemSettingResponseDto> createSetting(
            @Valid @RequestBody SystemSettingRequestDto requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createSetting(requestDto));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{key}")
    public ResponseEntity<SystemSettingResponseDto> getSettingByKey(@PathVariable String key) {
        return ResponseEntity.ok(service.getSettingByKey(key));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<SystemSettingResponseDto>> getAllSettings() {
        return ResponseEntity.ok(service.getAllSettings());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{key}")
    public ResponseEntity<SystemSettingResponseDto> updateSetting(
            @PathVariable String key,
            @Valid @RequestBody SystemSettingRequestDto requestDto) {
        return ResponseEntity.ok(service.updateSetting(key, requestDto));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{key}")
    public ResponseEntity<Void> deleteSetting(@PathVariable String key) {
        service.deleteSetting(key);
        return ResponseEntity.noContent().build();
    }
}