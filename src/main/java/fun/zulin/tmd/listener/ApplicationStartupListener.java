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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 应用启动监听器
 * 负责在应用启动时恢复未完成的下载任务
 */
@Slf4j
@Component
public class ApplicationStartupListener implements ApplicationListener<ApplicationReadyEvent> {

    private static final int MAX_ATTEMPTS = 60;
    private static final int RETRY_INTERVAL_SECONDS = 10;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("应用启动完成，开始恢复未完成的下载任务...");

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        AtomicInteger attempts = new AtomicInteger(0);

        scheduler.scheduleWithFixedDelay(() -> {
            try {
                int attempt = attempts.incrementAndGet();

                if (attempt > MAX_ATTEMPTS) {
                    log.warn("已重试 {} 次，Telegram客户端仍未就绪，停止恢复下载任务", MAX_ATTEMPTS);
                    scheduler.shutdown();
                    return;
                }

                if (Tmd.client == null) {
                    log.debug("Telegram客户端未就绪，等待初始化... (重试 {}/{})", attempt, MAX_ATTEMPTS);
                    return;
                }

                if (Tmd.savedMessagesChat == null) {
                    log.debug("Saved Messages聊天未就绪，等待初始化... (重试 {}/{})", attempt, MAX_ATTEMPTS);
                    return;
                }

                int currentActiveDownloads = DownloadManage.getActiveDownloadCount();
                if (currentActiveDownloads > 0) {
                    log.debug("当前有 {} 个活跃下载任务，跳过恢复检查", currentActiveDownloads);
                    scheduler.shutdown();
                    return;
                }

                log.info("Telegram客户端和Saved Messages聊天已就绪，开始恢复下载任务...");
                DownloadManage.startDownloading();
                log.info("下载任务恢复完成");
                scheduler.shutdown();

            } catch (Exception e) {
                log.error("恢复下载任务时发生错误", e);
            }
        }, 5, RETRY_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }
}
