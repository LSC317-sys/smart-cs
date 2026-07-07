package com.smartcs.knowledge.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "siliconflow")
public class SiliconFlowConfig {
    private String embeddingApiKey;
    private String embeddingBaseUrl;
    private String embeddingModel;
    private String chatApiKey;
    private String chatBaseUrl;
    private String chatModel;
}
