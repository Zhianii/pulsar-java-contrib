package org.apache.pulsar.admin.mcp.tools;

import java.util.*;
import java.util.Optional;

import org.apache.pulsar.admin.mcp.model.ToolMetadata;

public interface ToolRegistry {
    void registerTool(PulsarAdminTool tool);

    boolean unregisterTool(String toolName);

    Optional<PulsarAdminTool> getTool(String toolName);

    List<PulsarAdminTool> getAllTools();

    List<PulsarAdminTool> getToolsByCategory(String category);

    List<ToolMetadata> getToolMetadata();

    Optional<ToolMetadata> getToolMetadata(String toolName);

    boolean isToolRegistered(String toolName);

    int getToolCount();

    List<String> getCategories();

    void clear();
}