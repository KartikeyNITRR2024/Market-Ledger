package com.checkin.ingestion.user;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDto {

    private UUID id;
    private String email;
    private Role role;
    private boolean enabled;
    private LocalDateTime createdAt;
}