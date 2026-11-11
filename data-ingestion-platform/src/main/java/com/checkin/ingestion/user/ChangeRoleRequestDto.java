package com.checkin.ingestion.user;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangeRoleRequestDto {

    @NotNull(message = "Role must not be null")
    private Role role;
}