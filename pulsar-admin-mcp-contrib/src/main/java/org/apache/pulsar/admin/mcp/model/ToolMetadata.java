package org.apache.pulsar.admin.mcp.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

// 工具元数据
public record ToolMetadata (
        @JsonProperty("name") String name,
        @JsonProperty("description") String description,
        @JsonProperty("category") String category,
        @JsonProperty("parameters") ParameterDefinition parameters,
        @JsonProperty("readOnly") boolean readOnly,
        @JsonProperty("examples") List<ToolExample> examples) {

    public record ParameterDefinition (
            @JsonProperty("properties") Map<String,ParameterInfo> properties,
            @JsonProperty("required") List<String> required){}

    public record ParameterInfo (
            @JsonProperty("type") String type,
            @JsonProperty("description") String description,
            @JsonProperty("enum") List<String> enumValues,
            @JsonProperty("default") Object defaultValue,
            @JsonProperty("minimum") Number minimum,
            @JsonProperty("maximum") Number maximum,
            @JsonProperty("pattern") String pattern){}

    public record ToolExample (
            @JsonProperty("description") String description,
            @JsonProperty("parameters") Map<String, Object> parameters,
            @JsonProperty("expectedResult") String expectedResult){}

    public static Builder builder(){
        return new Builder();
    }


    public static class Builder {
        private String name;
        private String description;
        private String category;
        private Map<String, ParameterInfo> properties = Map.of();
        private List<String> required = List.of();
        private boolean readOnly = true;
        private List<ToolExample> examples = List.of();

        public Builder name(String name){
            this.name = name;
            return this;
        }

        public Builder description(String description){
            this.description = description;
            return this;
        }

        public Builder category(String category){
            this.category = category;
            return this;
        }

        public Builder properties(Map<String, ParameterInfo> properties){
            this.properties = properties;
            return this;
        }

        public Builder readOnly(boolean readOnly){
            this.readOnly = readOnly;
            return this;
        }

        public Builder required(List<String> required){
            this.required = required;
            return this;
        }

        public Builder examples(List<ToolExample> examples){
            this.examples = examples;
            return this;
        }

        public ToolMetadata build(){
            return new ToolMetadata(
                    name,
                    description,
                    category,
                    new ParameterDefinition(properties, required),
                    readOnly,
                    examples);
        }
    }
}

