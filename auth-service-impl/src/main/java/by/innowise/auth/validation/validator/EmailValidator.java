package by.innowise.auth.validation.validator;

import by.innowise.auth.service.UserService;
import by.innowise.auth.validation.api.EmailAvailable;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class EmailValidator implements ConstraintValidator<EmailAvailable, String> {

    private final UserService userService;

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        if (email == null || email.isBlank()) {
            return true;
        }
        return userService.isEmailFree(email);
    }
}
