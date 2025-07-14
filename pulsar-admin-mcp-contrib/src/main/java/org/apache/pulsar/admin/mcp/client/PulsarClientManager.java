package org.apache.pulsar.admin.mcp.client;

import org.apache.pulsar.admin.mcp.config.PulsarConfig;
import org.apache.pulsar.client.admin.PulsarAdmin;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;

@Component
public class PulsarClientManager {

    private static final Logger logger = LoggerFactory.getLogger(PulsarClientManager.class);
    private final PulsarAdmin adminClient;
    private final PulsarConfig config;

    public PulsarClientManager(PulsarConfig config){
        logger.info("Initializing PulsarClientManager:{}", config);
        this.config = config;

        try{
            this.adminClient = PulsarAdmin.builder()
                    .serviceHttpUrl(config.getAdminUrl())
                    .authentication(config.getAuthType(), config.getAuthParams())
                    .connectionTimeout(config.getConnectionTimeoutMs(), TimeUnit.MILLISECONDS)
                    .requestTimeout(config.getRequestTimeoutMs(), TimeUnit.MILLISECONDS)
                    .build();
            logger.info("Successfully initialized PulsarClientManager at {}", config.getAdminUrl());
        }catch (Exception ex){
            logger.error("Failed to initialize PulsarClientManager", ex);
            throw new RuntimeException("Failed to init PulsarClientManager", ex);
        }
    }

    public PulsarAdmin getAdminClient(){
        return adminClient;
    }

    public String getClusterName(){
        return config.getClusterName();
    }

    public void close(){
        if(adminClient != null){
            try{
                adminClient.close();
                logger.info("Successfully closed PulsarClientManager");
            }catch(Exception ex){
                logger.warn("Error closing Pulsar Admin Client", ex);
            }
        }
    }
}
