package fun.zulin.tmd.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 应用配置属性类
 * 统一管理应用配置项，支持配置验证和规范化
 */
@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "tmd")
public class TmdProperties {

    private DownloadConfig download = new DownloadConfig();
    private StorageConfig storage = new StorageConfig();
    private WebSocketConfig websocket = new WebSocketConfig();

    /**
     * 初始化后验证配置
     */
    public void validate() {
        download.validate();
        storage.validate();
        websocket.validate();
        log.info("TmdProperties 配置验证通过");
    }

    @Data
    public static class DownloadConfig {
        /**
         * Telegram下载优先级（1-56）
         */
        private int priority = 16;
        
        /**
         * 最大并发下载数
         */
        private int maxConcurrent = 3;
        
        /**
         * 下载超时时间（分钟）
         */
        private int timeoutMinutes = 30;
        
        /**
         * 重试次数
         */
        private int retryCount = 3;
        
        /**
         * 分块大小（字节）
         */
        private int chunkSize = 1048576; // 1MB
        
        /**
         * 进度更新间隔（毫秒）
         */
        private int progressUpdateInterval = 1000;
        
        /**
         * 默认优先级（用于内部调度）
         */
        private int defaultPriority = 3;

        /**
         * 验证并规范化下载配置
         */
        public void validate() {
            // 规范化优先级范围 (1-56)
            if (priority < 1) {
                log.warn("下载优先级 {} 小于最小值1，已设置为1", priority);
                priority = 1;
            } else if (priority > 56) {
                log.warn("下载优先级 {} 大于最大值56，已设置为56", priority);
                priority = 56;
            }

            // 规范化并发数 (1-10)
            if (maxConcurrent < 1) {
                log.warn("最大并发数 {} 小于最小值1，已设置为1", maxConcurrent);
                maxConcurrent = 1;
            } else if (maxConcurrent > 10) {
                log.warn("最大并发数 {} 大于最大值10，已设置为10", maxConcurrent);
                maxConcurrent = 10;
            }

            // 规范化超时时间 (1-120分钟)
            if (timeoutMinutes < 1) {
                log.warn("下载超时 {} 小于最小值1分钟，已设置为1", timeoutMinutes);
                timeoutMinutes = 1;
            } else if (timeoutMinutes > 120) {
                log.warn("下载超时 {} 大于最大值120分钟，已设置为120", timeoutMinutes);
                timeoutMinutes = 120;
            }

            // 规范化重试次数 (0-10)
            if (retryCount < 0) {
                log.warn("重试次数 {} 小于0，已设置为0", retryCount);
                retryCount = 0;
            } else if (retryCount > 10) {
                log.warn("重试次数 {} 大于10，已设置为10", retryCount);
                retryCount = 10;
            }

            // 规范化分块大小 (64KB-16MB)
            if (chunkSize < 65536) {
                log.warn("分块大小 {} 小于最小值64KB，已设置为64KB", chunkSize);
                chunkSize = 65536;
            } else if (chunkSize > 16777216) {
                log.warn("分块大小 {} 大于最大值16MB，已设置为16MB", chunkSize);
                chunkSize = 16777216;
            }
        }
    }

    @Data
    public static class StorageConfig {
        /**
         * 下载根目录
         */
        private String downloadDir = "downloads";
        
        /**
         * 数据目录
         */
        private String dataDir = "data";
        
        /**
         * 最大存储大小
         */
        private String maxStorageSize = "10GB";
        
        /**
         * 清理配置
         */
        private CleanupConfig cleanup = new CleanupConfig();

        /**
         * 验证存储配置
         */
        public void validate() {
            if (downloadDir == null || downloadDir.trim().isEmpty()) {
                log.warn("下载目录为空，已设置为默认值 'downloads'");
                downloadDir = "downloads";
            }
            
            if (dataDir == null || dataDir.trim().isEmpty()) {
                log.warn("数据目录为空，已设置为默认值 'data'");
                dataDir = "data";
            }
            
            cleanup.validate();
        }
    }

    @Data
    public static class CleanupConfig {
        /**
         * 过期天数
         */
        private int expiredDays = 7;
        
        /**
         * 是否自动清理
         */
        private boolean autoCleanup = true;

        /**
         * 验证清理配置
         */
        public void validate() {
            if (expiredDays < 1) {
                log.warn("过期天数 {} 小于1，已设置为1", expiredDays);
                expiredDays = 1;
            } else if (expiredDays > 365) {
                log.warn("过期天数 {} 大于365，已设置为365", expiredDays);
                expiredDays = 365;
            }
        }
    }

    @Data
    public static class WebSocketConfig {
        /**
         * 心跳间隔（毫秒）
         */
        private int heartbeatInterval = 30000;
        
        /**
         * 会话超时（毫秒）
         */
        private long sessionTimeout = 3600000;
        
        /**
         * 缓冲区大小
         */
        private int bufferSize = 8192;

        /**
         * 验证 WebSocket 配置
         */
        public void validate() {
            // 心跳间隔范围 (5秒-5分钟)
            if (heartbeatInterval < 5000) {
                log.warn("心跳间隔 {} 小于最小值5000ms，已设置为5000", heartbeatInterval);
                heartbeatInterval = 5000;
            } else if (heartbeatInterval > 300000) {
                log.warn("心跳间隔 {} 大于最大值300000ms，已设置为300000", heartbeatInterval);
                heartbeatInterval = 300000;
            }

            // 会话超时范围 (1分钟-24小时)
            if (sessionTimeout < 60000) {
                log.warn("会话超时 {} 小于最小值60000ms，已设置为60000", sessionTimeout);
                sessionTimeout = 60000;
            } else if (sessionTimeout > 86400000) {
                log.warn("会话超时 {} 大于最大值86400000ms，已设置为86400000", sessionTimeout);
                sessionTimeout = 86400000;
            }

            // 缓冲区大小范围 (1KB-64KB)
            if (bufferSize < 1024) {
                log.warn("缓冲区大小 {} 小于最小值1024，已设置为1024", bufferSize);
                bufferSize = 1024;
            } else if (bufferSize > 65536) {
                log.warn("缓冲区大小 {} 大于最大值65536，已设置为65536", bufferSize);
                bufferSize = 65536;
            }
        }
    }
    
    /**
     * 获取视频目录路径
     */
    public String getVideosDir() {
        return storage.getDownloadDir() + "/videos";
    }
    
    /**
     * 获取缩略图目录路径
     */
    public String getThumbnailsDir() {
        return storage.getDownloadDir() + "/thumbnails";
    }
    
    /**
     * 获取临时目录路径
     */
    public String getTempDir() {
        return storage.getDownloadDir() + "/temp";
    }
}
