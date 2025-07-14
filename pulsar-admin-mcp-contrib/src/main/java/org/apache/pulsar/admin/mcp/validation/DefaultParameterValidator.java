package org.apache.pulsar.admin.mcp.validation;

import org.apache.pulsar.admin.mcp.model.ToolSchema;
import org.apache.pulsar.admin.mcp.tools.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.validation.ObjectError;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class DefaultParameterValidator implements ParameterValidator {

    private static final Logger logger = LoggerFactory.getLogger(DefaultParameterValidator.class);

    private final ToolRegistry toolRegistry;

    public DefaultParameterValidator(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    @Override
    public ValidationResult validateParameters(String toolName, Map<String, Object> parameters) {
        logger.debug("Validating tool {}", toolName);

        Optional<ToolSchema> schemaOpt = toolRegistry.getToolSchema(toolName);
        if (schemaOpt.isEmpty()) {
            return ValidationResult.failure("Tool " + toolName + " does not exist");
        }

        ToolSchema schema = schemaOpt.get();
        List<String> errors = new ArrayList<>();

        ValidationResult requireResult =
                validateRequiredParameters(parameters, schema.parameters().required());
        if (!requireResult.valid()) {
            errors.addAll(requireResult.errors());
        }

        Object value;
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String paramName = entry.getKey();
            value = entry.getValue();

            ToolSchema.ParameterProperty property = schema.parameters().properties().get(paramName);
            if (property != null) {
                ValidationResult paramResult = validateParameterValue(paramName, value, property);
                if (!paramResult.valid()) {
                    errors.addAll(paramResult.errors());
                } else {
                    logger.warn("Unknown Parameter {} for tool {} ", paramName, toolName);
                }
            }
        }
        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }

    @Override
    public ValidationResult validateParameter(
            String parameterName, Object value, String expectedType){
        if(value == null){
            return ValidationResult.failure("Parameter " + parameterName + " is null");
        }
        return switch (expectedType.toLowerCase()){
            case "string" -> validateStringParameter(parameterName, value);
            case "integer" -> validateIntegerParameter(parameterName, value);
            case "boolean" -> validateBooleanParameter(parameterName, value);
            case "number" -> validateNumberParameter(parameterName, value);
            case "array" -> validateArrayParameter(parameterName, value);
            case "object" -> validateObjectParameter(parameterName, value);
            default -> ValidationResult.failure("Parameter " + expectedType + " is unknown");
        };
    }

    @Override
    public ValidationResult validateRequiredParameters(
        Map<String, Object> parameters, List<String> requiredParameters){
        List<String> errors = new ArrayList<>();

        for(String required : requiredParameters){
            if(parameters.containsKey(required) || parameters.get(required) == null){
                errors.add(required + " is required");
            }
        }
        return errors.isEmpty()  ? ValidationResult.failure(errors) : ValidationResult.success() ;
    }

    @Override
    public ValidationResult validateConstraints(String parameterName, Object value, Map<String, Object> constraints){
        List<String> errors = new ArrayList<>();

        if(constraints.containsKey("minimum") && value instanceof Number){
            Number min = (Number)constraints.get("minimum");
            if(((Number) value).doubleValue() < min.doubleValue()){
                errors.add("minimum must be greater than or equal to " + min);
            }
        }

        if(constraints.containsKey("maximum") && value instanceof Number){
            Number max = (Number)constraints.get("maximum");
            if(((Number) value).doubleValue() > max.doubleValue()){
                errors.add("maximum must be greater than or equal to " + max);
            }
        }

        if(constraints.containsKey("pattern") && value instanceof String){
            String pattern = (String) constraints.get("pattern");
            if(!Pattern.matches(pattern,(String) value)){
                errors.add("pattern must match regex " + pattern);
            }
        }

        if(constraints.containsKey("enum") && constraints.get("enum") instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> enumValues = (List<Object>) constraints.get("enum");
            if (!enumValues.contains(value)) {
                errors.add("enum must contains " + enumValues);
            }
        }
        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }

    private ValidationResult validateParameterValue(
            String paramName, Object value, ToolSchema.ParameterProperty property
    ){
        List<String> errors = new ArrayList<>();

        ValidationResult typeResult = validateParameter(paramName, value, property.type());
        if(!typeResult.valid()){
            errors.addAll(typeResult.errors());
        }

        Map<String, Object> constraints =
            Map.of(
                    "minimum",property.minimum() != null ? property.minimum(): 0,
                    "maximum",property.maximum() != null ? property.maximum() : Integer.MAX_VALUE,
                    "pattern",property.pattern() != null ? property.pattern() : "*",
                    "enum", property.enumValues() != null ? property.enumValues() : List.of());

            Map<String, Object> activeConstraints =
                    constraints.entrySet().stream()
                            .filter(
                    entry -> {
                            Object val = entry.getValue();
                            return val != null
                                    && !(val instanceof Number && ((Number) val).intValue() == 0)
                                    && !(val instanceof String && val.equals("*"))
                                    && !(val instanceof List && ((List) val).isEmpty());
                        })
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            if(!activeConstraints.isEmpty()){
                ValidationResult constraintResult = validateConstraints(paramName, value, activeConstraints);
                if(!constraintResult.valid()){
                    errors.addAll(constraintResult.errors());
                }
            }
            return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }

    private ValidationResult validateObjectParameter(String parameterName, Object value) {
        if(!(value instanceof Map)){
            return ValidationResult.failure("Parameter " + parameterName + " must be a object");
        }
        return ValidationResult.success();
    }

    private ValidationResult validateArrayParameter(String parameterName, Object value) {
        if(!(value instanceof List)){
            return ValidationResult.failure("Parameter " + parameterName + " must be a list");
        }
        return ValidationResult.success();
    }

    private ValidationResult validateBooleanParameter(String parameterName, Object value) {
        if(!(value instanceof Boolean)){
            if(value instanceof String){
                String str = ((String) value).toLowerCase();
                if("true".equals(str) || "false".equals(str)){
                    return  ValidationResult.success();
                }
            }
            return ValidationResult.failure("Parameter " + parameterName + " must be a boolean value");
        }
        return  ValidationResult.success();
    }

    private ValidationResult validateIntegerParameter(String parameterName, Object value) {
        if(!(value instanceof Integer) && !(value instanceof Long)){
            if(value instanceof String){
                try{
                    Integer.parseInt((String) value);
                    return ValidationResult.success();
                }catch (NumberFormatException e){
                    return ValidationResult.failure("parameter " + parameterName + " is not a integer number");
                }
            }else if(value instanceof Double){
                double d = (Double)value;
                if(d == Math.floor(d)){
                    return ValidationResult.success();
                }
            }
            return ValidationResult.failure("parameter " + parameterName + " is not a integer number");
        }
        return ValidationResult.success();
    }

    private ValidationResult validateNumberParameter(String parameterName, Object value){
        if(!(value instanceof Number)) {
            if (value instanceof String) {
                try {
                    Double.parseDouble((String) value);
                    return ValidationResult.success();
                } catch (NumberFormatException e) {
                    return ValidationResult.failure("parameter " + parameterName + " is not a number");
                }
            }
            return ValidationResult.failure("parameter " + parameterName + " is not a number");
        }
        return ValidationResult.success();
    }

    private ValidationResult validateStringParameter(String parameterName, Object value) {
        if(!(value instanceof String)){
            return ValidationResult.failure("String is required");
        }
        return ValidationResult.success();
    }
}
