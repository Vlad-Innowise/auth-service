package by.innowise.auth.validation.api;

import by.innowise.auth.validation.validator.EmailValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The provided email must not be assigned to any active user in the application.
 * {@code null} or {@code blank} elements are considered valid.
 */

@Constraint(validatedBy = EmailValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface EmailAvailable {

    String message() default "The user with such email address already exists!";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
