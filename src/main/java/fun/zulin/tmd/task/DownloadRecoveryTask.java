package fun.zulin.tmd.task;

import fun.zulin.tmd.data.item.DownloadItem;
import fun.zulin.tmd.data.item.DownloadItemServiceImpl;
import fun.zulin.tmd.data.item.DownloadState;
import fun.zulin.tmd.telegram.DownloadManage;
import fun.zulin.tmd.telegram.Tmd;
import fun.zulin.tmd.utils.SpringContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

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
            
            // 检查数据库中是否有未完成的任务（包括Failed状态的任务）
            var service = SpringContext.getBean(DownloadItemServiceImpl.class);
            var failedTasks = service.getFailedItemsFromDB();
            
            // 过滤掉已经在下载队列中的失败任务
            if (failedTasks != null && !failedTasks.isEmpty()) {
                failedTasks = failedTasks.stream()
                    .filter(task -> !DownloadManage.isItemInDownloadingQueue(task.getUniqueId()))
                    .collect(java.util.stream.Collectors.toList());
                log.debug("过滤后的失败任务数: {}", failedTasks.size());
            }
            
            int pendingTasks = DownloadManage.getItems().size();
            
            if (pendingTasks > 0) {
                log.info("发现 {} 个待处理的下载任务，开始恢复...", pendingTasks);
                DownloadManage.startDownloading();
            } else if (failedTasks != null && !failedTasks.isEmpty()) {
                log.info("发现 {} 个失败的下载任务，考虑重新尝试...", failedTasks.size());
                // 可以选择性地恢复失败的任务
                handleFailedTasksRecovery(failedTasks);
            }
            
        } catch (Exception e) {
            log.error("下载恢复检查时发生错误", e);
        }
    }
    
    /**
     * 处理失败任务的恢复
     * 可以选择性地将某些失败任务重新标记为待下载状态
     */
    private void handleFailedTasksRecovery(List<DownloadItem> failedTasks) {
        try {
            var service = SpringContext.getBean(DownloadItemServiceImpl.class);
            
            // 筛选出可以重试的任务（比如下载计数较少的），并且不在当前下载队列中
            failedTasks.stream()
                .filter(item -> item.getDownloadCount() != null && item.getDownloadCount() < 3)
                .filter(item -> !DownloadManage.isItemInDownloadingQueue(item.getUniqueId()))
                .forEach(item -> {
                    try {
                        // 再次检查，防止并发情况下的重复添加
                        if (DownloadManage.isItemInDownloadingQueue(item.getUniqueId())) {
                            log.debug("任务 {} 已在下载队列中，跳过恢复", item.getUniqueId());
                            return;
                        }
                        
                        log.info("尝试恢复失败任务: {} (失败次数: {}, UniqueId: {})", 
                            item.getFilename(), item.getDownloadCount(), item.getUniqueId());
                        
                        // 将状态改为Created，允许重新下载
                        item.setState(DownloadState.Created.name());
                        item.setCaption("重新尝试下载");
                        service.updateById(item);
                        
                        // 添加到下载队列
                        DownloadManage.addDownloadingItems(item);
                        
                    } catch (Exception e) {
                        log.error("恢复失败任务 {} 时出错", item.getUniqueId(), e);
                    }
                });
                
            // 如果有可恢复的任务，启动下载
            if (DownloadManage.getItems().size() > 0) {
                log.info("开始恢复 {} 个失败任务", DownloadManage.getItems().size());
                DownloadManage.startDownloading();
            }
            
        } catch (Exception e) {
            log.error("处理失败任务恢复时发生错误", e);
        }
    }
}