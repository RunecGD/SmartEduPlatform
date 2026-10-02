package org.example.core.config;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
@Configuration
public class MinioConfig {
    @Bean @Primary
    public MinioClient minioClient(@Value("${minio.url}") String url,
            @Value("${minio.access-key}") String accessKey,@Value("${minio.secret-key}") String secretKey) {
        return MinioClient.builder().endpoint(url).credentials(accessKey,secretKey).build();
    }
    @Bean("publicMinioClient")
    public MinioClient publicMinioClient(@Value("${minio.public-url:${minio.url}}") String url,
            @Value("${minio.access-key}") String accessKey,@Value("${minio.secret-key}") String secretKey) {
        return MinioClient.builder().endpoint(url).credentials(accessKey,secretKey).region("us-east-1").build();
    }
}
