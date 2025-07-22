package org.apache.pulsar.admin.mcp.tools;

import org.apache.pulsar.admin.mcp.model.ExecutionContext;
import org.apache.pulsar.admin.mcp.model.ToolMetadata;
import org.apache.pulsar.admin.mcp.model.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Primary
public class AnnotationBasedToolRegistry implements ToolRegistry {

    private static final Logger logger = LoggerFactory.getLogger(AnnotationBasedToolRegistry.class);

    private final Map<String, BaseToolWrapper> tools = new ConcurrentHashMap<>();
    private final ApplicationContext applicationContext;

    public AnnotationBasedToolRegistry(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        discoverAndRegisterTools();
    }

    private void discoverAndRegisterTools() {
        logger.info("Discovering and registering tools");

        Map<String, Object> allBeans = applicationContext.getBeansOfType(Object.class);

        int toolCount = 0;
        for(Object bean : allBeans.values()){
            Class<?> beanClass = bean.getClass();

            if(beanClass.getName().contains("$") ||
                    beanClass.getName().contains("java.") ||
                    beanClass.getName().startsWith("org.springframework.")){
                continue;
            }

            for(Method method : beanClass.getDeclaredMethods()){
                PulsarTool annotation = method.getAnnotation(PulsarTool.class);
                if(annotation != null){
                    registerAnnotatedTool(bean, method, annotation);
                    toolCount++;
                }
            }
        }
        logger.info("Registered {} tools", toolCount);
    }

    private void registerAnnotatedTool(Object bean, Method method, PulsarTool annotation) {
        String toolName = annotation.value();

        if(!isValidToolMethod(method)){
            logger.warn("Method {} is not valid", method.getName());
            return;
        }

        ToolMetadata metadata = createMethodFromAnnotation(annotation);

        AnnotationToolWrapper wrapper = new AnnotationToolWrapper(bean, method, metadata);

        tools.put(toolName, wrapper);
        logger.debug("Registered {} tool {} from {}", toolName, wrapper, bean.getClass().getName());
    }

    private boolean isValidToolMethod(Method method) {
        return method.getReturnType() == ToolMetadata.class &&
                method.getParameterCount() == 1 &&
                method.getParameterTypes()[0] == ExecutionContext.class;
    }

    private ToolMetadata createMethodFromAnnotation(PulsarTool annotation){
        Map<String, ToolMetadata.ParameterInfo> properties = new HashMap<>();

        for(String requiredParam : annotation.required()){
            properties.put(requiredParam, new ToolMetadata.ParameterInfo(
                    "string",
                    "Parameter:" + requiredParam,
                    null, null, null, null, null
            ));
        }
        ToolMetadata.ParameterDefinition parameters = new ToolMetadata.ParameterDefinition(
                properties,
                Arrays.asList(annotation.required())
        );

        return new ToolMetadata(
                annotation.value(),
                annotation.description().isEmpty() ? "Tool:" + annotation.value() : annotation.description(),
                annotation.category().isEmpty() ? "general" : annotation.category(),
                parameters,
                annotation.readOnly(),
                List.of()
        );
    }

    @Override
    public void registerTool(PulsarAdminTool tool) {
        String toolName = tool.getMetadata().name();
        LegacyToolWrapper wrapper = new LegacyToolWrapper(tool);
        tools.put(toolName, wrapper);
        logger.debug("Registered tool {}", toolName);
    }

    @Override
    public boolean unregisterTool(String toolName) {
        BaseToolWrapper removed = tools.remove(toolName);
        if(removed != null){
            logger.debug("Unregistered {} tool {}", removed, toolName);
            return true;
        }
        return false;
    }

    @Override
    public Optional<PulsarAdminTool> getTool(String name){
        BaseToolWrapper wrapper = tools.get(name);
        if(wrapper == null){
            return Optional.empty();
        }

        if(wrapper instanceof LegacyToolWrapper){
            return Optional.of(((LegacyToolWrapper) wrapper).getTool());
        }
        return Optional.of(new AnnotationToolAdapter(wrapper));
    }


    @Override
    public List<PulsarAdminTool> getAllTools(){
        return tools.values().stream()
                .map(wrapper -> {
                    if(wrapper instanceof LegacyToolWrapper){
                        return ((LegacyToolWrapper) wrapper).getTool();
                    }else{
                        return new AnnotationToolAdapter(wrapper);
                    }
                })
                .toList();
    }

    @Override
    public List<PulsarAdminTool> getToolsByCategory(String category){
        return tools.values().stream()
                .filter(wrapper -> category.equals(wrapper.getMetadata().category()))
                .map(wrapper -> {
                    if(wrapper instanceof LegacyToolWrapper){
                        return ((LegacyToolWrapper) wrapper).getTool();
                    }else{
                        return new AnnotationToolAdapter(wrapper);
                    }
                })
                .toList();
    }

    @Override
    public List<ToolMetadata> getToolMetadata(){
        return tools.values().stream()
                .map(BaseToolWrapper::getMetadata)
                .toList();
    }

    @Override
    public Optional<ToolMetadata> getToolMetadata(String toolName){
       BaseToolWrapper wrapper = tools.get(toolName);
       if(wrapper == null){
           return Optional.empty();
       }
       return Optional.of(wrapper.getMetadata());
    }

    @Override
    public boolean isToolRegistered(String toolName){
        return tools.containsKey(toolName);
    }

    @Override
    public int getToolCount(){
        return tools.size();
    }

    @Override
    public List<String> getCategories(){
        return tools.values().stream()
                .map(wrapper -> wrapper.getMetadata().category())
                .distinct()
                .sorted()
                .toList();
    }

    @Override
    public void clear(){
        tools.clear();
        logger.info("Cleared all registered tools");
    }

    public ToolResult executeTool(String toolName, ExecutionContext context){
        BaseToolWrapper wrapper = tools.get(toolName);
        if(wrapper == null){
            return ToolResult.error(
                    "Tool not found:" + toolName,
                    "TOOL_NOT_FOUND",
                    List.of("Use tools/list to see available tools")
            );
        }

        try{
            return wrapper.execute(context);
        }catch (Exception e){
            logger.error("Error executing tool {}: {}", toolName, e.getMessage());
            return ToolResult.error(
                    "Tool execution failed:" + e.getMessage(),
                    "EXECUTION_ERROR",
                    List.of("Check the tool parameters and try again")
            );
        }
    }

    private abstract static class BaseToolWrapper{
        protected  final ToolMetadata metadata;

        protected  BaseToolWrapper(ToolMetadata metadata){
            this.metadata = metadata;
        }

        public ToolMetadata getMetadata(){
            return metadata;
        }

        public abstract ToolResult execute(ExecutionContext context) throws Exception;
    }

    private static class AnnotationToolWrapper extends BaseToolWrapper{
        private final Object bean;
        private final Method method;

        public AnnotationToolWrapper(Object bean, Method method, ToolMetadata metadata){
            super(metadata);
            this.bean = bean;
            this.method = method;
            this.method.setAccessible(true);
        }

        @Override
        public ToolResult execute(ExecutionContext context) throws Exception{
            return (ToolResult) method.invoke(bean, context);
        }
    }

    private static class LegacyToolWrapper extends BaseToolWrapper{
        private final PulsarAdminTool tool;

        public LegacyToolWrapper(PulsarAdminTool tool){
            super(tool.getMetadata());
            this.tool = tool;
        }

        public PulsarAdminTool getTool(){
            return tool;
        }

        @Override
        public ToolResult execute(ExecutionContext context) throws Exception{
            return tool.execute(context);
        }
    }

    private static class AnnotationToolAdapter extends PulsarAdminTool{
        private final BaseToolWrapper wrapper;

        public AnnotationToolAdapter(BaseToolWrapper wrapper){
            super(null, wrapper.getMetadata().name());
            this.wrapper = wrapper;
        }

        @Override
        public ToolMetadata getMetadata(){
            return wrapper.getMetadata();
        }

        @Override
        public ToolResult execute(ExecutionContext context){
            try{
                return wrapper.execute(context);
            }catch (Exception e){
                return ToolResult.error(
                        "Execution failed:" + e.getMessage(),
                        "EXECUTION_ERROR",
                        List.of("Check the tool parameters and try again")
                );
            }
        }

        @Override
        public boolean isReadOnly(){
            return wrapper.getMetadata().readOnly();
        }
    }


}
