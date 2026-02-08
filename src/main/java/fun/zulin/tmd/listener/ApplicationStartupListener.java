package fun.zulin.tmd.listener;

import fun.zulin.tmd.telegram.DownloadManage;
import fun.zulin.tmd.telegram.Tmd;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 应用启动监听器
 * 负责在应用启动时恢复未完成的下载任务
 */
@Slf4j
@Component
public class ApplicationStartupListener implements ApplicationListener<ApplicationReadyEvent> {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("应用启动完成，开始恢复未完成的下载任务...");
        
        // 启动定时检查任务，直到Telegram客户端就绪
        scheduler.scheduleAtFixedRate(this::checkAndRecoverDownloads, 5, 10, TimeUnit.SECONDS);
    }
    
    /**
     * 检查并恢复下载任务
     * 只有当Telegram客户端和Saved Messages聊天都就绪时才执行
     */
    private void checkAndRecoverDownloads() {
        try {
            // 检查Telegram客户端是否就绪
            if (Tmd.client == null) {
                log.debug("Telegram客户端未就绪，等待初始化...");
                return;
            }
            
            // 检查Saved Messages聊天是否就绪
            if (Tmd.savedMessagesChat == null) {
                log.debug("Saved Messages聊天未就绪，等待初始化...");
                return;
            }
            
            // 检查当前是否有进行中的下载任务
            int currentActiveDownloads = DownloadManage.getActiveDownloadCount();
            if (currentActiveDownloads > 0) {
                log.debug("当前有 {} 个活跃下载任务，跳过恢复检查", currentActiveDownloads);
                // 停止定时任务
                scheduler.shutdown();
                return;
            }
            
            log.info("Telegram客户端和Saved Messages聊天已就绪，开始恢复下载任务...");
            
            // 恢复下载任务
            DownloadManage.startDownloading();
            
            log.info("下载任务恢复完成");
            
            // 停止定时任务
            scheduler.shutdown();
            
        } catch (Exception e) {
            log.error("恢复下载任务时发生错误", e);
        }
    }
}