package fun.zulin.tmd.task;

import fun.zulin.tmd.telegram.DownloadManage;
import fun.zulin.tmd.telegram.Tmd;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 下载恢复定时任务
 * 定期检查是否有需要恢复的下载任务
 */
@Slf4j
@Component
public class DownloadRecoveryTask {

    /**
     * 每5分钟检查一次是否有需要恢复的下载任务
     * 只在Telegram客户端就绪且有未完成任务时执行
     */
    @Scheduled(fixedDelay = 300000) // 5分钟
    public void checkAndRecoverDownloads() {
        try {
            // 检查Telegram客户端是否就绪
            if (Tmd.client == null || Tmd.savedMessagesChat == null) {
                log.debug("Telegram客户端未就绪，跳过下载恢复检查");
                return;
            }
            
            // 检查当前是否有进行中的下载任务
            int currentActiveDownloads = DownloadManage.getActiveDownloadCount();
            if (currentActiveDownloads > 0) {
                log.debug("当前有 {} 个活跃下载任务，跳过恢复检查", currentActiveDownloads);
                return;
            }
            
            // 检查数据库中是否有未完成的任务
            int pendingTasks = DownloadManage.getItems().size();
            if (pendingTasks > 0) {
                log.info("发现 {} 个待处理的下载任务，开始恢复...", pendingTasks);
                DownloadManage.startDownloading();
            }
            
        } catch (Exception e) {
            log.error("下载恢复检查时发生错误", e);
        }
    }
}