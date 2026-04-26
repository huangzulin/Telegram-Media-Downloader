package fun.zulin.tmd.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 配置初始化监听器
 * 在应用启动完成后验证配置
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConfigurationInitializer {

    private final TmdProperties tmdProperties;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("=== 开始验证 TMD 配置 ===");
        try {
            tmdProperties.validate();
            log.info("=== TMD 配置验证完成 ===");
        } catch (Exception e) {
            log.error("配置验证失败，应用可能无法正常工作", e);
        }
    }
}
