package com.checkin.ingestion.user;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangeStatusRequestDto {

    @NotNull(message = "Enabled flag must not be null")
    private Boolean enabled;
}