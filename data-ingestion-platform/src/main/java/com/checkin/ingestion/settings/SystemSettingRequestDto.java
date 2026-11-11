package com.checkin.ingestion.settings;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemSettingRequestDto {

    @NotBlank(message = "Setting key cannot be blank")
    private String id;

    @NotNull(message = "Setting value cannot be null")
    private Integer value;

    @NotNull(message = "UpdatedBy UUID cannot be null")
    private UUID updatedBy;
}