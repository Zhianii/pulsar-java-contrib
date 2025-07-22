package org.apache.pulsar.admin.mcp.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 统一的工具返回格式
public record ToolResult(
    boolean success,
    Object data,
    String errorCode,
    String errorMessage,
    List<String> suggestions,
    Map<String, Object> metadata) {

    public static ToolResult success(Object data){
        return new ToolResult(true, data, null, null, List.of(), Map.of());
    }

    public static ToolResult error(String errorMessage, String errorCode, List<String> suggestions){
        return new ToolResult(false, null, errorCode, errorMessage, suggestions, Map.of());
    }

    public ToolResult withExecutionTime(long executionTime){
        Map<String, Object> newMetadata = new HashMap<>(metadata);
        newMetadata.put("executionTime", executionTime + "ms");
        return new ToolResult(success, data, errorCode, errorMessage, suggestions, newMetadata);
    }

    public ToolResult withCluster(String clusterName){
        Map<String, Object> newMetadata = new HashMap<>(metadata);
        newMetadata.put("clusterName", clusterName);
        return new ToolResult(success, data, errorCode, errorMessage, suggestions, newMetadata);
    }

    private static String categorizeError(Throwable error){
        if(error instanceof IllegalArgumentException){
            return "INVALID_PARAMETER";
        }else if(error.getMessage().contains("not found") ||
                error.getMessage().contains("does not exist")){
            return "RESOURCE_NOT_FOUND";
        }else if(error.getMessage().contains("already exists")){
            return "ALREADY_EXISTS";
        }else if(error.getMessage().contains("unauthorized") ||
                error.getMessage().contains("forbidden")){
            return "UNAUTHORIZED";
        } else if(error.getMessage().contains("connect")){
            return "CONNECT_ERROR";
        }else{
            return "INTERNAL_ERROR";
        }
    }
}
