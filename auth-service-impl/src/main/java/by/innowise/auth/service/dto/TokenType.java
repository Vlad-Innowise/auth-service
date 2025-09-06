package by.innowise.auth.service.dto;

import lombok.Getter;

public enum TokenType {

    ACCESS("access"),
    REFRESH("refresh");

    @Getter
    private final String type;

    TokenType(String type) {
        this.type = type;
    }
}
