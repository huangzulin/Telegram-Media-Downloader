package fun.zulin.tmd.telegram;

import cn.hutool.core.collection.CollectionUtil;
import fun.zulin.tmd.common.constant.SystemConstants;
import fun.zulin.tmd.config.TmdProperties;
import fun.zulin.tmd.data.item.DownloadItem;
import fun.zulin.tmd.data.item.DownloadItemServiceImpl;
import fun.zulin.tmd.data.item.DownloadState;
import fun.zulin.tmd.utils.SpringContext;
import fun.zulin.tmd.utils.VideoProcessor;
import it.tdlight.jni.TdApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
public class DownloadManage {

    private static final List<DownloadItem> downloadingItems = new CopyOnWriteArrayList<>();

    private static final AtomicInteger activeDownloads = new AtomicInteger(0);

    private static volatile int DOWNLOAD_PRIORITY = 16;

    private static volatile int MAX_CONCURRENT_DOWNLOADS = 3;

    private static volatile int DOWNLOAD_TIMEOUT_MINUTES = 30;

    private static volatile ThreadPoolExecutor executorService;

    private static final List<StateChangeListener> stateChangeListeners = new CopyOnWriteArrayList<>();

    private static volatile boolean initialized = false;

    private static volatile SimpMessagingTemplate cachedMessagingTemplate;

    /**
     * 初始化下载管理器
     * 在应用启动或配置变更时调用
     */
    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        
        // 从配置获取参数
        try {
            var properties = SpringContext.getBean(TmdProperties.class);
            MAX_CONCURRENT_DOWNLOADS = properties.getDownload().getMaxConcurrent();
            DOWNLOAD_PRIORITY = properties.getDownload().getPriority();
            DOWNLOAD_TIMEOUT_MINUTES = properties.getDownload().getTimeoutMinutes();
        } catch (Exception e) {
            log.warn("无法获取配置，使用默认值: 最大并发数={}, 优先级={}, 超时={}分钟", 
                    MAX_CONCURRENT_DOWNLOADS, DOWNLOAD_PRIORITY, DOWNLOAD_TIMEOUT_MINUTES);
        }

        // 创建线程池（核心线程空闲超时需要非零的 keepAliveTime）
        executorService = new ThreadPoolExecutor(
                MAX_CONCURRENT_DOWNLOADS,
                MAX_CONCURRENT_DOWNLOADS,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                r -> {
                    Thread t = new Thread(r);
                    t.setName("download-thread-" + System.nanoTime());
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        executorService.allowCoreThreadTimeOut(true);
        
        initialized = true;
        log.info("DownloadManage 初始化完成: 最大并发数={}, 优先级={}, 超时={}分钟", 
                MAX_CONCURRENT_DOWNLOADS, DOWNLOAD_PRIORITY, DOWNLOAD_TIMEOUT_MINUTES);
    }

    /**
     * 优雅关闭下载管理器
     */
    public static synchronized void shutdown() {
        if (!initialized || executorService == null) {
            return;
        }

        log.info("开始优雅关闭 DownloadManage...");
        
        // 停止接受新任务
        executorService.shutdown();
        
        try {
            // 等待现有任务完成，最多等待30秒
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                // 强制停止
                executorService.shutdownNow();
                log.warn("下载线程池强制关闭");
            } else {
                log.info("下载线程池正常关闭");
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
            log.warn("关闭过程被中断");
        }
        
        initialized = false;
    }

    /**
     * 获取执行器
     */
    static ExecutorService getExecutorService() {
        if (!initialized) {
            initialize();
        }
        return executorService;
    }

    public static List<DownloadItem> getItems() {
        return new ArrayList<>(downloadingItems);
    }

    /**
     * 获取当前活跃下载数量
     */
    public static int getActiveDownloadCount() {
        return activeDownloads.get();
    }

    /**
     * 获取最大并发下载数
     */
    public static int getMaxConcurrentDownloads() {
        return MAX_CONCURRENT_DOWNLOADS;
    }

    /**
     * 状态变更监听器接口
     */
    public interface StateChangeListener {
        void onStateChanged(DownloadItem item, String oldState, String newState);
    }

    /**
     * 添加状态变更监听器
     */
    public static void addListener(StateChangeListener listener) {
        if (listener != null) {
            stateChangeListeners.add(listener);
            log.debug("添加状态监听器，当前监听器数量: {}", stateChangeListeners.size());
        }
    }

    /**
     * 移除状态变更监听器
     */
    public static void removeListener(StateChangeListener listener) {
        if (listener != null) {
            stateChangeListeners.remove(listener);
            log.debug("移除状态监听器，当前监听器数量: {}", stateChangeListeners.size());
        }
    }

    /**
     * 触发状态变更事件
     */
    private static void fireStateChanged(DownloadItem item, String oldState, String newState) {
        if (!stateChangeListeners.isEmpty()) {
            for (StateChangeListener listener : stateChangeListeners) {
                try {
                    listener.onStateChanged(item, oldState, newState);
                } catch (Exception e) {
                    log.error("状态变更监听器执行异常", e);
                }
            }
        }
    }

    /**
     * 下载文件
     *
     * @param item 下载项
     */
    public static void download(DownloadItem item) {
        if (item == null) {
            log.warn("下载项不能为空");
            return;
        }

        // 确保初始化
        if (!initialized) {
            initialize();
        }

        getExecutorService().submit(() -> {
            try {
                // 检查并发限制
                while (activeDownloads.get() >= MAX_CONCURRENT_DOWNLOADS) {
                    log.debug("达到并发限制，等待中: {}", item.getUniqueId());
                    Thread.sleep(500);
                }

                activeDownloads.incrementAndGet();
                log.info("开始下载文件: {}, 当前活跃下载数: {}", item.getFilename(), activeDownloads.get());

                CountDownLatch countDownLatch = new CountDownLatch(1);

                // 更新状态为下载中
                try {
                    var service = SpringContext.getBean(DownloadItemServiceImpl.class);
                    var saveItem = service.getByUniqueId(item.getUniqueId());
                    if (saveItem != null && !DownloadState.Downloading.name().equals(saveItem.getState())) {
                        String oldState = saveItem.getState();
                        saveItem.setState(DownloadState.Downloading.name());
                        service.updateById(saveItem);
                        log.info("更新下载项状态为下载中: {}", item.getFilename());

                        // 触发状态变更事件
                        fireStateChanged(saveItem, oldState, saveItem.getState());

                        // 立即推送状态更新
                        pushStateUpdate(saveItem);
                    }
                } catch (Exception e) {
                    log.warn("更新下载状态失败: {}", item.getUniqueId(), e);
                }

                Tmd.client.send(new TdApi.DownloadFile(item.getFileId(),
                        DOWNLOAD_PRIORITY, 0, 0, true), result -> {
                    try {
                        if (result.isError()) {
                            log.error("下载失败 {}: {}", item.getUniqueId(), result.getError().message);
                            handleDownloadError(item, result.getError().message);
                        } else {
                            var service = SpringContext.getBean(DownloadItemServiceImpl.class);
                            var saveItem = service.getByUniqueId(item.getUniqueId());

                            // 执行文件重命名操作
                            boolean renameSuccess = renameDownloadedFile(saveItem, result.get().local.path);

                            if (!renameSuccess) {
                                log.error("文件重命名失败，不更新完成状态: {}", saveItem.getFilename());
                                handleDownloadError(saveItem, "文件重命名失败");
                                return;
                            }

                            // 如果是视频文件，直接生成本地截图作为缩略图
                            String thumbnailFilename = null;
                            if (isVideoFile(saveItem.getFilename())) {
                                log.info("开始生成视频 {} 的缩略图", saveItem.getFilename());
                                thumbnailFilename = generateVideoThumbnail(saveItem);
                                if (thumbnailFilename != null) {
                                    saveItem.setThumbnail(thumbnailFilename);
                                    log.info("成功设置视频封面: {} -> {}", saveItem.getFilename(), thumbnailFilename);
                                } else {
                                    log.warn("设置视频封面失败: {}", saveItem.getFilename());
                                }
                            }

                            // 确认文件确实存在后再更新完成状态
                            Path finalFilePath = Paths.get(SystemConstants.File.getVideosDirPath(), saveItem.getFilename());
                            if (!Files.exists(finalFilePath)) {
                                log.error("下载完成但文件不存在，不更新状态: {}", finalFilePath);
                                handleDownloadError(saveItem, "文件未正确保存到目标位置");
                                return;
                            }

                            // 更新下载完成状态
                            String oldState = saveItem.getState();
                            saveItem.setState(DownloadState.Complete.name());
                            saveItem.setDownloadedSize(result.get().size);
                            saveItem.setProgress(100.0f);
                            saveItem.setCompleteTime(LocalDateTime.now(ZoneId.of("Asia/Shanghai")));

                            boolean updateSuccess = service.updateById(saveItem);
                            if (!updateSuccess) {
                                log.error("更新下载完成状态失败: {}", saveItem.getFilename());
                                return;
                            }

                            log.info("成功更新下载完成状态: {} (重命名: {}) 文件大小: {} bytes",
                                    saveItem.getFilename(), renameSuccess, result.get().size);

                            // 触发状态变更事件
                            fireStateChanged(saveItem, oldState, saveItem.getState());

                            // 推送状态更新
                            pushStateUpdate(saveItem);

                            log.info("下载完成: {} (重命名: {})", item.getFilename(), renameSuccess ? "成功" : "失败");
                        }
                    } finally {
                        // 从下载队列中移除
                        removeDownloadingItems(item.getUniqueId());
                        countDownLatch.countDown();
                    }
                });

                try {
                    if (!countDownLatch.await(DOWNLOAD_TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
                        log.warn("下载超时: {}", item.getUniqueId());
                        handleDownloadTimeout(item);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("下载被中断: {}", item.getUniqueId(), e);
                    handleDownloadError(item, "下载被中断");
                }
            } catch (Exception e) {
                log.error("下载过程中发生异常: {}", item.getUniqueId(), e);
                handleDownloadError(item, e.getMessage());
            } finally {
                activeDownloads.decrementAndGet();
                log.info("下载结束: {}, 当前活跃下载数: {}", item.getFilename(), activeDownloads.get());
            }
        });
    }

    /**
     * 生成视频缩略图
     * 直接使用本地视频截取第一帧作为缩略图
     *
     * @param item 下载项
     * @return 缩略图文件名，失败返回null
     */
    private static String generateVideoThumbnail(DownloadItem item) {
        try {
            log.info("开始生成视频缩略图: {} (ID: {})", item.getFilename(), item.getId());

            // 直接生成视频截图
            String videoPath = SystemConstants.File.getVideosDirPath() + "/" + item.getFilename();
            log.info("尝试生成视频截图: {}", videoPath);
            String generatedThumbnail = VideoProcessor.extractFirstFrameAsThumbnail(videoPath);
            if (generatedThumbnail != null) {
                log.info("成功生成视频截图作为封面: {}", generatedThumbnail);
                return generatedThumbnail;
            }

            log.warn("无法生成视频封面: {}", item.getFilename());
            return null;

        } catch (Exception e) {
            log.error("生成视频缩略图时发生异常: {}", item.getUniqueId(), e);
            return null;
        }
    }

    /**
     * 处理下载错误
     */
    private static void handleDownloadError(DownloadItem item, String errorMessage) {
        if (item == null || item.getUniqueId() == null) {
            log.warn("下载项或唯一ID为空，无法更新错误状态");
            return;
        }
        
        try {
            var service = SpringContext.getBean(DownloadItemServiceImpl.class);
            var saveItem = service.getByUniqueId(item.getUniqueId());
            if (saveItem == null) {
                log.warn("未找到下载项记录: {}", item.getUniqueId());
                return;
            }
            
            saveItem.setState(DownloadState.Failed.name());
            saveItem.setCaption(errorMessage != null ? errorMessage : "未知错误");
            service.updateById(saveItem);

            // 推送状态更新
            pushStateUpdate(saveItem);
        } catch (Exception e) {
            log.error("更新下载错误状态失败: {}", item.getUniqueId(), e);
        }
    }

    /**
     * 处理下载超时
     */
    private static void handleDownloadTimeout(DownloadItem item) {
        handleDownloadError(item, "下载超时");
    }

    /**
     * 重命名下载完成的文件
     *
     * @param item               下载项
     * @param downloadedFilePath 下载完成的文件路径
     * @return 重命名是否成功
     */
    private static boolean renameDownloadedFile(DownloadItem item, String downloadedFilePath) {
        try {
            if (downloadedFilePath == null || downloadedFilePath.isEmpty()) {
                log.warn("下载文件路径为空: {}", item.getUniqueId());
                return false;
            }

            Path sourcePath = Paths.get(downloadedFilePath);
            if (!Files.exists(sourcePath)) {
                log.warn("源文件不存在: {}", downloadedFilePath);
                return false;
            }

            // 确保视频目录存在
            Path videosDir = Paths.get(SystemConstants.File.getVideosDirPath());
            if (!Files.exists(videosDir)) {
                Files.createDirectories(videosDir);
                log.info("创建目录: {}", videosDir.toAbsolutePath());
            }

            // 构建目标文件路径
            Path targetPath = videosDir.resolve(item.getFilename());

            // 如果目标文件已存在，先删除
            if (Files.exists(targetPath)) {
                Files.delete(targetPath);
                log.info("删除已存在的目标文件: {}", targetPath.getFileName());
            }

            // 移动文件到目标位置
            Files.move(sourcePath, targetPath);
            log.info("文件重命名成功: {} -> {}",
                    sourcePath.getFileName(), targetPath.getFileName());

            return true;

        } catch (IOException e) {
            log.error("文件重命名失败: {} -> {}",
                    downloadedFilePath, item.getFilename(), e);
            return false;
        } catch (Exception e) {
            log.error("文件重命名过程中发生异常: {}", item.getUniqueId(), e);
            return false;
        }
    }

    /**
     * 启动下载（用于应用重启时恢复下载）
     * 恢复数据库中未完成的下载任务
     */
    public static void startDownloading() {
        if (Tmd.client == null) {
            log.warn("Telegram客户端未就绪，无法恢复下载任务");
            return;
        }

        if (Tmd.savedMessagesChat == null) {
            log.warn("Saved Messages聊天未就绪，无法恢复下载任务");
            return;
        }

        try {
            var service = SpringContext.getBean(DownloadItemServiceImpl.class);

            var items = service.getDownloadingItemsFromDB();
            log.info("查询到的未完成任务数: {}", items.size());

            if (items == null || items.isEmpty()) {
                log.info("没有发现未完成的下载任务");
                return;
            }

            downloadingItems.clear();
            downloadingItems.addAll(CollectionUtil.emptyIfNull(items));

            log.info("发现 {} 个未完成的下载任务，开始恢复...", downloadingItems.size());

            downloadingItems.forEach(item -> {
                log.info("恢复下载任务: {} ({}) 状态: {}",
                        item.getFilename(), item.getUniqueId(), item.getState());

                // 检查消息是否存在
                long targetChatId = item.getChatId() != null ? item.getChatId() : Tmd.savedMessagesChat.id;
                Tmd.client.send(new TdApi.GetMessage(targetChatId, item.getMassageId()), message -> {
                    if (message.isError()) {
                        log.error("获取消息失败 {}: {}", item.getUniqueId(), message.getError().message);
                        handleDownloadError(item, "消息不存在或已被删除");
                        return;
                    }

                    // 检查消息内容类型
                    if (message.get().content instanceof TdApi.MessageVideo video) {
                        var fileId = video.video.video.id;
                        item.setFileId(fileId);

                        // 根据状态决定是否重新开始下载
                        if (DownloadState.Created.name().equals(item.getState()) ||
                                DownloadState.Failed.name().equals(item.getState()) ||
                                DownloadState.Pause.name().equals(item.getState())) {
                            log.info("重新开始下载任务: {} ({})", item.getFilename(), item.getUniqueId());
                            DownloadManage.download(item);
                        } else if (DownloadState.Downloading.name().equals(item.getState())) {
                            log.info("任务已在下载中: {} ({})，跳过重复启动", item.getFilename(), item.getUniqueId());
                        }
                    } else {
                        log.warn("消息内容不是视频类型 {}: {}", item.getUniqueId(),
                                message.get().content.getClass().getSimpleName());
                        handleDownloadError(item, "消息内容类型不支持");
                    }
                });
            });

            log.info("下载任务恢复流程启动完成");
        } catch (Exception e) {
            log.error("恢复下载任务时发生异常", e);
        }
    }

    /**
     * 检查下载项是否仍在内存队列中
     * 用于判断是否应该恢复该任务
     */
    public static boolean isItemInDownloadingQueue(String uniqueId) {
        return downloadingItems.stream()
                .anyMatch(item -> item.getUniqueId().equals(uniqueId));
    }

    /**
     * 清理已删除的下载项（防止应用重启时重建）
     * 这个方法可以在适当的时候调用，比如定期清理
     */
    public static void cleanupDeletedItems() {
        try {
            var service = SpringContext.getBean(DownloadItemServiceImpl.class);
            var dbItems = service.getDownloadingItemsFromDB();
            var dbItemIds = dbItems.stream()
                    .map(DownloadItem::getUniqueId)
                    .collect(java.util.stream.Collectors.toSet());

            downloadingItems.removeIf(item -> !dbItemIds.contains(item.getUniqueId()));

            log.info("清理完成，当前内存队列大小: {}", downloadingItems.size());
        } catch (Exception e) {
            log.error("清理已删除下载项时发生异常", e);
        }
    }

    /**
     * 推送状态更新到前端
     *
     * @param item 更新的下载项
     */
    private static void pushStateUpdate(DownloadItem item) {
        try {
            if (cachedMessagingTemplate == null) {
                cachedMessagingTemplate = SpringContext.getBean(SimpMessagingTemplate.class);
            }
            if (cachedMessagingTemplate != null) {
                cachedMessagingTemplate.convertAndSend(SystemConstants.WebSocket.DOWNLOADING_TOPIC,
                        getItems());
            }
        } catch (Exception e) {
            log.debug("推送状态更新失败: {}", item.getUniqueId(), e);
        }
    }

    public static void addDownloadingItems(DownloadItem item) {
        downloadingItems.add(item);
    }

    public static void removeDownloadingItems(String uniqueId) {
        int beforeSize = downloadingItems.size();
        downloadingItems.removeIf(d -> d.getUniqueId().equals(uniqueId));
        int afterSize = downloadingItems.size();

        if (beforeSize != afterSize) {
            log.info("从下载队列中移除项: {}, 队列大小: {} -> {}", uniqueId, beforeSize, afterSize);
        }
    }

    /**
     * 判断文件是否为视频文件
     *
     * @param filename 文件名
     * @return 是否为视频文件
     */
    private static boolean isVideoFile(String filename) {
        if (filename == null || filename.isEmpty()) {
            return false;
        }
        String lowerFilename = filename.toLowerCase();
        return lowerFilename.endsWith(".mp4") ||
                lowerFilename.endsWith(".avi") ||
                lowerFilename.endsWith(".mov") ||
                lowerFilename.endsWith(".wmv") ||
                lowerFilename.endsWith(".flv") ||
                lowerFilename.endsWith(".mkv") ||
                lowerFilename.endsWith(".webm") ||
                lowerFilename.endsWith(".m4v");
    }

    /**
     * 更新下载进度
     *
     * @param uniqueId       唯一id
     * @param downloadedSize 已下载
     */
    public static void updateProgress(String uniqueId, Long downloadedSize) {

        for (DownloadItem item : downloadingItems) {
            if (uniqueId.equals(item.getUniqueId())) {

                // 添加null检查，确保downloadCount不为null
                int currentDownloadCount = item.getDownloadCount() != null ? item.getDownloadCount() : 0;

                if (currentDownloadCount < 5) {
                    item.setDownloadCount(currentDownloadCount + 1);
                } else {
                    item.setDownloadCount(0);
                    var downloadDiff = downloadedSize - item.getDownloadedSize();
                    var timeDiff = Duration.between(item.getDownloadUpdateTime(), LocalDateTime.now(ZoneId.of("Asia/Shanghai"))).toMillis();

                    item.setDownloadUpdateTime(LocalDateTime.now(ZoneId.of("Asia/Shanghai")));
                    item.setDownloadedSize(downloadedSize);

                    var speed = (((float) downloadDiff / timeDiff) * 1000);

                    item.setDownloadBytePerSec((long) speed);
                }
            }
        }


    }

}