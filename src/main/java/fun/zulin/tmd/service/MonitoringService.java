package fun.zulin.tmd.service;

import fun.zulin.tmd.telegram.DownloadManage;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 应用监控服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringService {
    
    private final MeterRegistry meterRegistry;
    
    @PostConstruct
    public void initMetrics() {
        // 注册活跃下载数指标
        Gauge.builder("tmd.downloads.active", DownloadManage::getActiveDownloadCount)
            .description("当前活跃下载任务数")
            .register(meterRegistry);
            
        // 注册最大并发下载数指标
        Gauge.builder("tmd.downloads.max_concurrent", DownloadManage::getMaxConcurrentDownloads)
            .description("最大并发下载数")
            .register(meterRegistry);
            
        // 注册下载队列大小指标
        Gauge.builder("tmd.downloads.queue_size", () -> (double) DownloadManage.getItems().size())
            .description("下载队列大小")
            .register(meterRegistry);
            
        log.info("监控指标注册完成");
    }
}