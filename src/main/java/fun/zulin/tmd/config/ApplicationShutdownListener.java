package fun.zulin.tmd.config;

import fun.zulin.tmd.telegram.Tmd;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 应用关闭监听器
 * 确保Telegram客户端优雅关闭
 */
@Slf4j
@Component
public class ApplicationShutdownListener {

    @EventListener
    public void onContextClosedEvent(ContextClosedEvent event) {
        log.info("应用程序正在关闭，开始清理资源...");
        
        try {
            // 关闭Telegram客户端
            if (Tmd.client != null) {
                log.info("正在关闭Telegram客户端...");
                CompletableFuture<Void> closeFuture = Tmd.client.closeAsync();
                
                // 等待最多10秒关闭
                try {
                    closeFuture.get(10, TimeUnit.SECONDS);
                    log.info("Telegram客户端已成功关闭");
                } catch (Exception e) {
                    log.warn("Telegram客户端关闭超时或出错: {}", e.getMessage());
                }
            }
            
            log.info("资源清理完成");
        } catch (Exception e) {
            log.error("关闭过程中出现错误: {}", e.getMessage());
        }
    }
}