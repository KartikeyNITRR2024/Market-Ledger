package com.checkin.ingestion.auth;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponseDto {
    private String token;
    private String type = "Bearer";
    private String email;
    private String role;
}
