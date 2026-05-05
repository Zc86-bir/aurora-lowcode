package com.aurora.core.infrastructure.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OssConfig.OssProperties.class)
public class OssConfig {

    @Bean
    public OSS ossClient(OssProperties props) {
        return new OSSClientBuilder().build(
            props.getEndpoint(),
            props.getAccessKeyId(),
            props.getAccessKeySecret()
        );
    }

    @Bean
    public String ossBucketName(OssProperties props) {
        return props.getBucketName();
    }

    @ConfigurationProperties(prefix = "aurora.storage.oss")
    public static class OssProperties {
        private String endpoint;
        private String bucketName;
        private String accessKeyId;
        private String accessKeySecret;

        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

        public String getBucketName() { return bucketName; }
        public void setBucketName(String bucketName) { this.bucketName = bucketName; }

        public String getAccessKeyId() { return accessKeyId; }
        public void setAccessKeyId(String accessKeyId) { this.accessKeyId = accessKeyId; }

        public String getAccessKeySecret() { return accessKeySecret; }
        public void setAccessKeySecret(String accessKeySecret) { this.accessKeySecret = accessKeySecret; }
    }
}
