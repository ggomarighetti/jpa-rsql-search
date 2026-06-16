package io.github.ggomarighetti.searchhelper.exception;

import io.github.ggomarighetti.searchhelper.validation.RuleViolation;
import java.util.List;
import java.util.Objects;

final class ValidationExceptionSupport {
    private ValidationExceptionSupport() {
    }

    static String requireCode(String code) {
        if (Objects.requireNonNull(code, "code must not be null").isBlank()) {
            throw new IllegalArgumentException("code must not be blank");
        }
        return code;
    }

    static List<RuleViolation> copyViolations(List<RuleViolation> violations) {
        return List.copyOf(Objects.requireNonNull(violations, "violations must not be null"));
    }
}
