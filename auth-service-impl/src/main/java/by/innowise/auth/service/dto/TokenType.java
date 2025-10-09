package by.innowise.auth.service.dto;

import lombok.Getter;

import java.util.Arrays;

public enum TokenType {

    ACCESS("access"),
    REFRESH("refresh");

    @Getter
    private final String type;

    TokenType(String type) {
        this.type = type;
    }

    public static TokenType fromType(String typeName) {
        return Arrays.stream(TokenType.values())
                     .filter(e -> e.getType().equalsIgnoreCase(typeName))
                     .findFirst()
                     .orElseThrow(() -> new IllegalArgumentException(
                             "Haven't found a TokenType for [%s] value".formatted(typeName)));
    }
}
