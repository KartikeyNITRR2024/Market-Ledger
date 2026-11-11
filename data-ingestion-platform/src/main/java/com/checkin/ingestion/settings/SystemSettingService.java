package com.checkin.ingestion.settings;

import java.util.List;

public interface SystemSettingService {

    SystemSettingResponseDto createSetting(SystemSettingRequestDto requestDto);

    SystemSettingResponseDto getSettingByKey(String id);

    List<SystemSettingResponseDto> getAllSettings();

    SystemSettingResponseDto updateSetting(String id, SystemSettingRequestDto requestDto);

    void deleteSetting(String key);
}