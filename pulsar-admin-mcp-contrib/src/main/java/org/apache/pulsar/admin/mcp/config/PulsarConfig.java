package org.apache.pulsar.admin.mcp.config;

import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "pulsar.client")
public class PulsarConfig {

    @Setter
    private String adminUrl = "http://localhost:8080";
    private String serviceUrl = "http://localhost:6650";
    private String clusterName = "standalone";
    private String authType = "";
    private String authParams = "";
    private int connectionTimeoutMs = 5000;
    private int requestTimeoutMs = 30000;

    public String getAdminUrl() {
        return adminUrl;
    }

    public void setAdminUrl(String adminUrl) {
        this.adminUrl = adminUrl;
    }

    public String getServiceUrl() {
        return serviceUrl;
    }

    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    public String getClusterName(){
       return clusterName;
    }

    public void setClusterName(String clusterName){
        this.clusterName = clusterName;
    }

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(String authType){
        this.authType = authType;
    }

    public int getConnectionTimeoutMs() {
        return connectionTimeoutMs;
    }

    public void setConnectionTimeoutMs(int connectionTimeoutMs) {
        this.connectionTimeoutMs = connectionTimeoutMs;
    }

    public int getRequestTimeoutMs() {
        return requestTimeoutMs;
    }

    public void setRequestTimeoutMs(int requestTimeoutMs) {
        this.requestTimeoutMs = requestTimeoutMs;
    }

    @Override
    public String toString(){
        return "PulsarConfig" +
                "adminUrl=" + adminUrl + '\'' +
                ",serviceUrl='" + serviceUrl + '\'' +
                ",clusterName='" + clusterName + '\'' +
                ",authType='" + authType + '\'' +
                ",connectionTimeoutMs=" + connectionTimeoutMs +
                ",requestTimeoutMs=" + requestTimeoutMs +
                '}';
    }

    public String getAuthParams() {
        return authParams;
    }
}
