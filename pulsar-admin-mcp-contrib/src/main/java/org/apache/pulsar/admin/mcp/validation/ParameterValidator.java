package org.apache.pulsar.admin.mcp.validation;

import java.util.List;
import java.util.Map;

public interface ParameterValidator {
    record ValidationResult(boolean valid, List<String> errors) {
        public static ValidationResult success() {
            return new ValidationResult(true, List.of());
        }

        public static ValidationResult failure(List<String> errors) {
            return new ValidationResult(false, errors);
        }

        public static ValidationResult failure(String error) {
            return new ValidationResult(false, List.of(error));
        }
    }
    ValidationResult validateParameters(String toolName, Map<String, Object> parameters);

    ValidationResult validateParameter(String parameterName, Object value, String expectedType);

    ValidationResult validateRequiredParameters(Map<String, Object> parameters, List<String> requiredParameters);

    ValidationResult validateConstraints(String parameterName, Object value, Map<String, Object> constraints);

}

