package com.checkin.ingestion.settings;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.checkin.ingestion.common.exceptions.ResourceNotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemSettingServiceImpl implements SystemSettingService {

    private final SystemSettingRepository repository;

    @Override
    @Transactional
    public SystemSettingResponseDto createSetting(SystemSettingRequestDto requestDto) {
        log.info("Initiating creation of system setting with key: '{}' by user: {}", requestDto.getId(), requestDto.getUpdatedBy());

        if (repository.existsById(requestDto.getId())) {
            log.warn("System setting creation failed: key '{}' already exists", requestDto.getId());
            throw new IllegalArgumentException("Setting with key '" + requestDto.getId() + "' already exists");
        }

        SystemSetting setting = SystemSetting.builder()
                .id(requestDto.getId())
                .value(requestDto.getValue())
                .updatedBy(requestDto.getUpdatedBy())
                .updatedAt(LocalDateTime.now())
                .build();

        SystemSetting saved = repository.save(setting);
        log.info("Successfully created system setting '{}' with initial value: {}", saved.getId(), saved.getValue());
        return mapToResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public SystemSettingResponseDto getSettingByKey(String key) {
        log.debug("Fetching system setting for key: '{}'", key);

        SystemSetting setting = repository.findById(key)
                .orElseThrow(() -> {
                    log.warn("Lookup failed: system setting with key '{}' not found", key);
                    return new ResourceNotFoundException("Setting with key '" + key + "' not found");
                });

        log.debug("Found system setting '{}': value = {}", setting.getId(), setting.getValue());
        return mapToResponseDto(setting);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemSettingResponseDto> getAllSettings() {
        log.info("Fetching all active system settings");
        List<SystemSetting> settings = repository.findAll();
        log.info("Retrieved {} system setting records from database", settings.size());

        return settings.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SystemSettingResponseDto updateSetting(String key, SystemSettingRequestDto requestDto) {
        log.info("Attempting to update system setting '{}' by user: {}", key, requestDto.getUpdatedBy());

        SystemSetting setting = repository.findById(key)
                .orElseThrow(() -> {
                    log.error("Update aborted: system setting key '{}' does not exist", key);
                    return new ResourceNotFoundException("Setting with key '" + key + "' not found");
                });

        Integer oldValue = setting.getValue();
        setting.setValue(requestDto.getValue());
        setting.setUpdatedBy(requestDto.getUpdatedBy());
        setting.setUpdatedAt(LocalDateTime.now());

        SystemSetting updated = repository.save(setting);
        log.info("System setting '{}' successfully updated from value {} to {} by user: {}",
                key, oldValue, updated.getValue(), requestDto.getUpdatedBy());

        return mapToResponseDto(updated);
    }

    @Override
    @Transactional
    public void deleteSetting(String key) {
        log.warn("Request received to delete system setting with key: '{}'", key);

        if (!repository.existsById(key)) {
            log.error("Deletion failed: setting with key '{}' does not exist", key);
            throw new ResourceNotFoundException("Setting with key '" + key + "' not found");
        }

        repository.deleteById(key);
        log.info("System setting '{}' was permanently deleted", key);
    }

    private SystemSettingResponseDto mapToResponseDto(SystemSetting entity) {
        return SystemSettingResponseDto.builder()
                .id(entity.getId())
                .value(entity.getValue())
                .updatedBy(entity.getUpdatedBy())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
