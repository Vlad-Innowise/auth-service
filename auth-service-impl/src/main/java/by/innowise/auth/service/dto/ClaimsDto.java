package by.innowise.auth.service.dto;

import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.Map;

@Value
@Builder
public class ClaimsDto {

    String subject;

    Date issuedAt;

    Date expiresAt;

    Map<String, Object> customClaims;
}
