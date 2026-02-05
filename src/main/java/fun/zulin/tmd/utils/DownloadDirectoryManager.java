package fun.zulin.tmd.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 下载目录管理器
 * 负责管理下载目录，处理U盘/移动硬盘掉线等异常情况
 */
@Slf4j
@Component
public class DownloadDirectoryManager {

    @Value("${DOWNLOAD_DIR:downloads}")
    private String downloadDirEnv;

    private Path downloadPath;
    private Path videosPath;
    private Path thumbnailsPath;
    private Path tempPath;
    
    /** 目录可用性状态 */
    private final AtomicBoolean directoryAvailable = new AtomicBoolean(true);
    
    /** 上次检测时间 */
    private volatile LocalDateTime lastCheckTime = LocalDateTime.now();
    
    /** 目录访问锁 */
    private final ReentrantReadWriteLock directoryLock = new ReentrantReadWriteLock();
    
    /** 监控调度器 */
    private ScheduledExecutorService monitorScheduler;
    
    /** 监控间隔（秒） */
    private static final int MONITOR_INTERVAL = 30;
    
    /** 快速检测间隔（秒）- 当目录不可用时 */
    private static final int FAST_MONITOR_INTERVAL = 5;

    @PostConstruct
    public void init() {
        log.info("初始化下载目录管理器...");
        
        try {
            initializePaths();
            createDirectoryStructure();
            startMonitoring();
            log.info("下载目录管理器初始化完成");
        } catch (Exception e) {
            log.error("下载目录管理器初始化失败", e);
            throw new RuntimeException("下载目录初始化失败", e);
        }
    }

    /**
     * 初始化路径
     */
    private void initializePaths() {
        downloadPath = Paths.get(downloadDirEnv).toAbsolutePath();
        videosPath = downloadPath.resolve("videos");
        thumbnailsPath = downloadPath.resolve("thumbnails");
        tempPath = downloadPath.resolve("temp");
        
        log.info("下载目录配置: {}", downloadPath);
        log.info("视频目录: {}", videosPath);
        log.info("缩略图目录: {}", thumbnailsPath);
        log.info("临时目录: {}", tempPath);
    }

    /**
     * 创建目录结构
     */
    private void createDirectoryStructure() throws IOException {
        try {
            directoryLock.writeLock().lock();
            
            // 检查并创建主目录
            if (!ensureDirectoryExists(downloadPath, "下载主目录")) {
                throw new IOException("无法创建下载主目录: " + downloadPath);
            }
            
            // 创建子目录
            ensureDirectoryExists(videosPath, "视频目录");
            ensureDirectoryExists(thumbnailsPath, "缩略图目录");
            ensureDirectoryExists(tempPath, "临时目录");
            
            log.info("目录结构创建完成");
            
        } finally {
            directoryLock.writeLock().unlock();
        }
    }

    /**
     * 确保目录存在且可写
     */
    private boolean ensureDirectoryExists(Path path, String description) {
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("创建{}: {}", description, path);
            }
            
            if (!Files.isDirectory(path)) {
                log.error("{}不是目录: {}", description, path);
                return false;
            }
            
            // 测试写入权限
            Path testFile = path.resolve(".write_test_" + System.currentTimeMillis());
            try {
                Files.write(testFile, "test".getBytes());
                Files.deleteIfExists(testFile);
                log.debug("{}可写: {}", description, path);
                return true;
            } catch (IOException e) {
                log.error("无法写入{}: {}", description, path, e);
                return false;
            }
            
        } catch (IOException e) {
            log.error("创建{}失败: {}", description, path, e);
            return false;
        }
    }

    /**
     * 启动目录监控
     */
    private void startMonitoring() {
        monitorScheduler = Executors.newSingleThreadScheduledExecutor(
            r -> new Thread(r, "directory-monitor")
        );
        
        monitorScheduler.scheduleWithFixedDelay(
            this::monitorDirectoryHealth,
            MONITOR_INTERVAL,
            MONITOR_INTERVAL,
            TimeUnit.SECONDS
        );
        
        log.info("目录健康监控已启动，检查间隔: {}秒", MONITOR_INTERVAL);
    }

    /**
     * 监控目录健康状态
     */
    private void monitorDirectoryHealth() {
        try {
            boolean wasAvailable = directoryAvailable.get();
            boolean nowAvailable = checkDirectoryAccessibility();
            
            if (wasAvailable && !nowAvailable) {
                // 目录从可用变为不可用
                handleDirectoryUnavailable();
            } else if (!wasAvailable && nowAvailable) {
                // 目录从不可用变为可用
                handleDirectoryAvailable();
            }
            
            lastCheckTime = LocalDateTime.now();
            
        } catch (Exception e) {
            log.error("目录健康检查异常", e);
        }
    }

    /**
     * 检查目录可访问性
     */
    private boolean checkDirectoryAccessibility() {
        try {
            directoryLock.readLock().lock();
            
            // 检查主目录是否存在且可访问
            if (!Files.exists(downloadPath) || !Files.isDirectory(downloadPath)) {
                return false;
            }
            
            // 检查子目录
            if (!Files.exists(videosPath) || !Files.exists(thumbnailsPath) || !Files.exists(tempPath)) {
                return false;
            }
            
            // 测试读写能力
            Path testFile = downloadPath.resolve(".health_check_" + System.currentTimeMillis());
            try {
                Files.write(testFile, "health".getBytes());
                byte[] content = Files.readAllBytes(testFile);
                Files.deleteIfExists(testFile);
                return new String(content).equals("health");
            } catch (IOException e) {
                return false;
            }
            
        } catch (Exception e) {
            log.debug("目录访问性检查失败: {}", e.getMessage());
            return false;
        } finally {
            directoryLock.readLock().unlock();
        }
    }

    /**
     * 处理目录不可用情况
     */
    private void handleDirectoryUnavailable() {
        directoryAvailable.set(false);
        log.warn("⚠ 下载目录不可访问: {}", downloadPath);
        
        // 发送通知给前端
        try {
            var template = SpringContext.getBean(org.springframework.messaging.simp.SimpMessagingTemplate.class);
            template.convertAndSend("/topic/directory-status", 
                java.util.Map.of(
                    "status", "unavailable",
                    "message", "下载目录不可访问，请检查U盘或移动硬盘连接",
                    "timestamp", LocalDateTime.now().toString()
                ));
        } catch (Exception e) {
            log.debug("发送目录状态通知失败", e);
        }
        
        // 调整监控频率
        adjustMonitorFrequency(true);
    }

    /**
     * 处理目录恢复可用情况
     */
    private void handleDirectoryAvailable() {
        directoryAvailable.set(true);
        log.info("✅ 下载目录恢复正常: {}", downloadPath);
        
        // 重新创建可能丢失的目录结构
        try {
            createDirectoryStructure();
        } catch (IOException e) {
            log.error("重新创建目录结构失败", e);
            directoryAvailable.set(false);
            return;
        }
        
        // 发送恢复通知
        try {
            var template = SpringContext.getBean(org.springframework.messaging.simp.SimpMessagingTemplate.class);
            template.convertAndSend("/topic/directory-status",
                java.util.Map.of(
                    "status", "available",
                    "message", "下载目录已恢复",
                    "timestamp", LocalDateTime.now().toString()
                ));
        } catch (Exception e) {
            log.debug("发送目录状态通知失败", e);
        }
        
        // 恢复正常监控频率
        adjustMonitorFrequency(false);
    }

    /**
     * 调整监控频率
     */
    private void adjustMonitorFrequency(boolean fastMode) {
        if (monitorScheduler != null && !monitorScheduler.isShutdown()) {
            monitorScheduler.shutdown();
        }
        
        monitorScheduler = Executors.newSingleThreadScheduledExecutor(
            r -> new Thread(r, "directory-monitor")
        );
        
        int interval = fastMode ? FAST_MONITOR_INTERVAL : MONITOR_INTERVAL;
        monitorScheduler.scheduleWithFixedDelay(
            this::monitorDirectoryHealth,
            interval,
            interval,
            TimeUnit.SECONDS
        );
        
        log.info("目录监控频率调整为: {}秒", interval);
    }

    /**
     * 获取下载目录路径（带可用性检查）
     */
    public Path getDownloadPath() throws IOException {
        checkAvailability();
        directoryLock.readLock().lock();
        try {
            return downloadPath;
        } finally {
            directoryLock.readLock().unlock();
        }
    }

    /**
     * 获取视频目录路径
     */
    public Path getVideosPath() throws IOException {
        checkAvailability();
        directoryLock.readLock().lock();
        try {
            return videosPath;
        } finally {
            directoryLock.readLock().unlock();
        }
    }

    /**
     * 获取缩略图目录路径
     */
    public Path getThumbnailsPath() throws IOException {
        checkAvailability();
        directoryLock.readLock().lock();
        try {
            return thumbnailsPath;
        } finally {
            directoryLock.readLock().unlock();
        }
    }

    /**
     * 获取临时目录路径
     */
    public Path getTempPath() throws IOException {
        checkAvailability();
        directoryLock.readLock().lock();
        try {
            return tempPath;
        } finally {
            directoryLock.readLock().unlock();
        }
    }

    /**
     * 检查目录可用性
     */
    private void checkAvailability() throws IOException {
        if (!directoryAvailable.get()) {
            throw new IOException("下载目录不可用，请检查U盘或移动硬盘连接");
        }
    }

    /**
     * 获取目录状态信息
     */
    public DirectoryStatus getStatus() {
        return DirectoryStatus.builder()
            .available(directoryAvailable.get())
            .path(downloadPath.toString())
            .lastCheckTime(lastCheckTime)
            .videosPath(videosPath.toString())
            .thumbnailsPath(thumbnailsPath.toString())
            .tempPath(tempPath.toString())
            .build();
    }

    /**
     * 关闭管理器
     */
    public void shutdown() {
        if (monitorScheduler != null) {
            monitorScheduler.shutdown();
            try {
                if (!monitorScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    monitorScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                monitorScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        log.info("下载目录管理器已关闭");
    }

    /**
     * 目录状态信息类
     */
    public static class DirectoryStatus {
        private boolean available;
        private String path;
        private LocalDateTime lastCheckTime;
        private String videosPath;
        private String thumbnailsPath;
        private String tempPath;

        public static DirectoryStatusBuilder builder() {
            return new DirectoryStatusBuilder();
        }

        // Getters and Setters
        public boolean isAvailable() { return available; }
        public void setAvailable(boolean available) { this.available = available; }
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public LocalDateTime getLastCheckTime() { return lastCheckTime; }
        public void setLastCheckTime(LocalDateTime lastCheckTime) { this.lastCheckTime = lastCheckTime; }
        public String getVideosPath() { return videosPath; }
        public void setVideosPath(String videosPath) { this.videosPath = videosPath; }
        public String getThumbnailsPath() { return thumbnailsPath; }
        public void setThumbnailsPath(String thumbnailsPath) { this.thumbnailsPath = thumbnailsPath; }
        public String getTempPath() { return tempPath; }
        public void setTempPath(String tempPath) { this.tempPath = tempPath; }

        public static class DirectoryStatusBuilder {
            private boolean available;
            private String path;
            private LocalDateTime lastCheckTime;
            private String videosPath;
            private String thumbnailsPath;
            private String tempPath;

            public DirectoryStatusBuilder available(boolean available) {
                this.available = available;
                return this;
            }

            public DirectoryStatusBuilder path(String path) {
                this.path = path;
                return this;
            }

            public DirectoryStatusBuilder lastCheckTime(LocalDateTime lastCheckTime) {
                this.lastCheckTime = lastCheckTime;
                return this;
            }

            public DirectoryStatusBuilder videosPath(String videosPath) {
                this.videosPath = videosPath;
                return this;
            }

            public DirectoryStatusBuilder thumbnailsPath(String thumbnailsPath) {
                this.thumbnailsPath = thumbnailsPath;
                return this;
            }

            public DirectoryStatusBuilder tempPath(String tempPath) {
                this.tempPath = tempPath;
                return this;
            }

            public DirectoryStatus build() {
                DirectoryStatus status = new DirectoryStatus();
                status.setAvailable(available);
                status.setPath(path);
                status.setLastCheckTime(lastCheckTime);
                status.setVideosPath(videosPath);
                status.setThumbnailsPath(thumbnailsPath);
                status.setTempPath(tempPath);
                return status;
            }
        }
    }
}