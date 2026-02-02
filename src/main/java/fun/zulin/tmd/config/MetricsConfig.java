package fun.zulin.tmd.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 监控指标配置
 */
@Configuration
public class MetricsConfig {
    
    @Bean
    MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config()
            .commonTags("application", "telegram-media-downloader")
            .commonTags("version", "1.0")
            .commonTags("environment", System.getenv("SPRING_PROFILES_ACTIVE") != null ? 
                System.getenv("SPRING_PROFILES_ACTIVE") : "default");
    }
}