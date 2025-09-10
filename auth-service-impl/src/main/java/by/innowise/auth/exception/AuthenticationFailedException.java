package by.innowise.auth.exception;

import by.innowise.common.library.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class AuthenticationFailedException extends ApplicationException {

    public AuthenticationFailedException(String message, HttpStatus httpStatus, Throwable cause) {
        super(message, httpStatus, cause);
    }

    public AuthenticationFailedException(String message, HttpStatus httpStatus) {
        super(message, httpStatus);
    }
}
