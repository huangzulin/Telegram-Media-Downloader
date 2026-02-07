package fun.zulin.tmd.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 应用降级配置
 * 当核心功能（如Telegram）不可用时提供备用方案
 */
@Slf4j
@Configuration
@EnableScheduling
public class FallbackConfiguration {

    /**
     * 当Telegram功能被禁用时的降级处理
     */
    @Bean
    @ConditionalOnProperty(name = "telegram.enabled", havingValue = "false")
    public TelegramFallbackService telegramFallbackService() {
        log.info("Telegram功能已禁用，启用降级服务");
        return new TelegramFallbackService();
    }

    /**
     * Telegram降级服务实现
     */
    public static class TelegramFallbackService {
        public TelegramFallbackService() {
            log.info("Telegram降级服务已启动");
        }
        
        public void logStatus() {
            log.info("应用正在降级模式下运行 - Telegram功能不可用");
        }
    }
}