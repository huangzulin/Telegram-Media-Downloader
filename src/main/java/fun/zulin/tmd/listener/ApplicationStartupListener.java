package fun.zulin.tmd.listener;

import fun.zulin.tmd.telegram.DownloadManage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * 应用启动监听器
 * 负责在应用启动时恢复未完成的下载任务
 */
@Slf4j
@Component
public class ApplicationStartupListener implements ApplicationListener<ApplicationReadyEvent> {

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("应用启动完成，开始恢复未完成的下载任务...");
        
        try {
            // 延迟执行以确保Telegram客户端已完全初始化
            Thread.sleep(5000);
            
            // 恢复下载任务
            DownloadManage.startDownloading();
            
            log.info("下载任务恢复完成");
        } catch (Exception e) {
            log.error("恢复下载任务时发生错误", e);
        }
    }
}