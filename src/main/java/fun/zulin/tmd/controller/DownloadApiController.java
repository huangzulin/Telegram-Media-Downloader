package fun.zulin.tmd.controller;

import fun.zulin.tmd.common.constant.SystemConstants;
import fun.zulin.tmd.common.exception.ApiResponse;
import fun.zulin.tmd.data.item.DownloadItem;
import fun.zulin.tmd.data.item.DownloadItemService;
import fun.zulin.tmd.data.item.DownloadItemServiceImpl;
import fun.zulin.tmd.data.item.DownloadState;
import fun.zulin.tmd.dto.BatchDownloadRequest;
import fun.zulin.tmd.dto.BatchDownloadResponse;
import fun.zulin.tmd.dto.TelegramLinkRequest;
import fun.zulin.tmd.telegram.DownloadManage;
import fun.zulin.tmd.telegram.Tmd;
import fun.zulin.tmd.utils.DataCleanupUtil;
import fun.zulin.tmd.utils.SpringContext;
import it.tdlight.jni.TdApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 下载管理API控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/downloads")
@RequiredArgsConstructor
public class DownloadApiController {

    private final DownloadItemService downloadItemService;

    /**
     * 获取已完成的下载项
     */
    @GetMapping("/completed")
    public ApiResponse<List<DownloadItem>> getCompletedDownloads() {
        try {
            List<DownloadItem> items = downloadItemService.getDownloadedItem();
            return ApiResponse.success(items);
        } catch (Exception e) {
            log.error("获取已完成下载项失败", e);
            return ApiResponse.error(500, "获取失败: " + e.getMessage());
        }
    }

    /**
     * 获取进行中的下载项
     */
    @GetMapping("/downloading")
    public ApiResponse<List<DownloadItem>> getDownloadingDownloads() {
        try {
            List<DownloadItem> items = DownloadManage.getItems();
            return ApiResponse.success(items);
        } catch (Exception e) {
            log.error("获取进行中下载项失败", e);
            return ApiResponse.error(500, "获取失败: " + e.getMessage());
        }
    }

    /**
     * 清理已完成的下载项
     */
    @PostMapping("/clear-completed")
    public ApiResponse<Void> clearCompletedDownloads() {
        try {
            // 获取所有已完成的下载项
            List<DownloadItem> completedItems = downloadItemService.getDownloadedItem();

            int deletedCount = 0;
            int fileDeletedCount = 0;

            for (DownloadItem item : completedItems) {
                // 删除对应的文件
                if (deleteDownloadedFile(item)) {
                    fileDeletedCount++;
                }

                // 从数据库中删除记录
                if (downloadItemService.removeByUniqueId(item.getUniqueId())) {
                    deletedCount++;
                }
            }

            log.info("清理完成：删除了 {} 条记录，{} 个文件", deletedCount, fileDeletedCount);
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("清理已完成下载项失败", e);
            return ApiResponse.error(500, "清理失败: " + e.getMessage());
        }
    }

    /**
     * 暂停所有下载
     */
    @PostMapping("/pause-all")
    public ApiResponse<Void> pauseAllDownloads() {
        try {
            // 这里可以添加暂停所有下载的逻辑
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("暂停所有下载失败", e);
            return ApiResponse.error(500, "操作失败: " + e.getMessage());
        }
    }

    /**
     * 暂停指定下载项
     */
    @PostMapping("/{uniqueId}/pause")
    public ApiResponse<Void> pauseDownload(@PathVariable String uniqueId) {
        try {
            // 这里可以添加暂停指定下载的逻辑
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("暂停下载失败: {}", uniqueId, e);
            return ApiResponse.error(500, "操作失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有标签
     */
    @GetMapping("/tags")
    public ApiResponse<List<String>> getAllTags() {
        try {
            List<String> tags = downloadItemService.getAllUniqueTags();
            return ApiResponse.success(tags);
        } catch (Exception e) {
            log.error("获取标签列表失败", e);
            return ApiResponse.error(500, "获取失败: " + e.getMessage());
        }
    }

    /**
     * 根据标签搜索下载项
     */
    @GetMapping("/search/tag/{tag}")
    public ApiResponse<List<DownloadItem>> searchByTag(@PathVariable String tag) {
        try {
            List<DownloadItem> items = downloadItemService.searchByTag(tag);
            return ApiResponse.success(items);
        } catch (Exception e) {
            log.error("按标签搜索失败: {}", tag, e);
            return ApiResponse.error(500, "搜索失败: " + e.getMessage());
        }
    }

    /**
     * 为下载项添加标签
     */
    @PostMapping("/{uniqueId}/tags")
    public ApiResponse<Void> addTags(@PathVariable String uniqueId, @RequestBody List<String> tags) {
        try {
            DownloadItem item = downloadItemService.getByUniqueId(uniqueId);
            if (item == null) {
                return ApiResponse.error(404, "未找到指定的下载项");
            }

            // 合并现有标签和新标签
            String existingTags = item.getTags() != null ? item.getTags() : "";
            List<String> existingTagList = Arrays.stream(existingTags.split(","))
                    .map(String::trim)
                    .filter(t -> !t.isEmpty())
                    .collect(Collectors.toList());

            // 添加新标签（去重）
            tags.stream()
                    .map(String::trim)
                    .filter(tag -> !tag.isEmpty() && !existingTagList.contains(tag))
                    .forEach(existingTagList::add);

            // 更新数据库
            item.setTags(String.join(",", existingTagList));
            downloadItemService.updateById(item);

            log.info("为下载项 {} 添加标签: {}", uniqueId, tags);
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("添加标签失败: {}", uniqueId, e);
            return ApiResponse.error(500, "添加失败: " + e.getMessage());
        }
    }

    /**
     * 删除下载项的指定标签
     */
    @DeleteMapping("/{uniqueId}/tags/{tag}")
    public ApiResponse<Void> removeTag(@PathVariable String uniqueId, @PathVariable String tag) {
        try {
            DownloadItem item = downloadItemService.getByUniqueId(uniqueId);
            if (item == null) {
                return ApiResponse.error(404, "未找到指定的下载项");
            }

            if (item.getTags() == null || item.getTags().isEmpty()) {
                return ApiResponse.success(null);
            }

            // 移除指定标签
            List<String> tagList = Arrays.stream(item.getTags().split(","))
                    .map(String::trim)
                    .filter(t -> !t.isEmpty() && !t.equals(tag))
                    .collect(Collectors.toList());

            item.setTags(String.join(",", tagList));
            downloadItemService.updateById(item);

            log.info("从下载项 {} 移除标签: {}", uniqueId, tag);
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("移除标签失败: {} - {}", uniqueId, tag, e);
            return ApiResponse.error(500, "移除失败: " + e.getMessage());
        }
    }

    /**
     * 删除下载项
     */
    @DeleteMapping("/{uniqueId}")
    public ApiResponse<Void> deleteDownload(@PathVariable String uniqueId) {
        try {
            // 先查询要删除的下载项
            DownloadItem item = downloadItemService.getByUniqueId(uniqueId);
            if (item == null) {
                return ApiResponse.error(404, "未找到指定的下载项");
            }

            // 先从内存下载队列中移除（防止重建）
            DownloadManage.removeDownloadingItems(uniqueId);

            // 删除对应的文件
            boolean fileDeleted = deleteDownloadedFile(item);

            // 从数据库中删除
            boolean dbDeleted = downloadItemService.removeByUniqueId(uniqueId);

            if (dbDeleted) {
                log.info("成功删除下载项: {} (文件删除: {})", uniqueId, fileDeleted ? "成功" : "失败");
                return ApiResponse.success(null);
            } else {
                return ApiResponse.error(500, "数据库删除失败");
            }
        } catch (Exception e) {
            log.error("删除下载项失败: {}", uniqueId, e);
            return ApiResponse.error(500, "删除失败: " + e.getMessage());
        }
    }

    /**
     * 批量下载指定范围内的消息（通过链接组合方式）
     */
    @PostMapping("/batch")
    public ApiResponse<BatchDownloadResponse> batchDownload(@RequestBody BatchDownloadRequest request) {
        try {
            // 参数验证
            if (request.getChatId() == null || request.getChatId().trim().isEmpty()) {
                return ApiResponse.error(400, "频道/群组名称不能为空");
            }

            if (request.getStartMessageId() == null || request.getEndMessageId() == null) {
                return ApiResponse.error(400, "起始和结束消息ID都不能为空");
            }

            if (request.getStartMessageId() > request.getEndMessageId()) {
                return ApiResponse.error(400, "起始消息ID不能大于结束消息ID");
            }

            // 验证并发数和间隔参数
            if (request.getConcurrent() == null || request.getConcurrent() < 1 || request.getConcurrent() > 10) {
                return ApiResponse.error(400, "并发数必须在1-10之间");
            }

            if (request.getInterval() == null || request.getInterval() < 0 || request.getInterval() > 5000) {
                return ApiResponse.error(400, "间隔时间必须在0-5000毫秒之间");
            }

            // 检查Telegram客户端是否就绪
            if (Tmd.client == null || Tmd.savedMessagesChat == null) {
                return ApiResponse.error(503, "Telegram客户端未就绪，请先登录");
            }

            log.info("收到批量下载请求: 频道={}, 消息ID范围={}~{}, 并发数={}, 间隔={}ms",
                    request.getChatId(), request.getStartMessageId(), request.getEndMessageId(),
                    request.getConcurrent(), request.getInterval());

            // 异步处理批量下载（通过链接组合方式）
            processBatchDownloadByLinks(request);

            // 计算总任务数
            int totalCount = (int) (request.getEndMessageId() - request.getStartMessageId() + 1);

            BatchDownloadResponse response = new BatchDownloadResponse(
                    totalCount, 0, 0, "批量下载任务已提交，正在后台处理中..."
            );

            return ApiResponse.success(response);

        } catch (Exception e) {
            log.error("处理批量下载请求失败", e);
            return ApiResponse.error(500, "处理失败: " + e.getMessage());
        }
    }



    /**
     * 异步处理批量下载（通过链接组合方式）
     */
    private void processBatchDownloadByLinks(BatchDownloadRequest request) {
        new Thread(() -> {
            try {
                log.info("开始处理批量下载任务（链接方式）: 频道={}, 消息ID范围={}~{}, 并发数={}, 间隔={}ms",
                        request.getChatId(), request.getStartMessageId(), request.getEndMessageId(),
                        request.getConcurrent(), request.getInterval());

                // 创建信号量控制并发数
                Semaphore semaphore = new Semaphore(request.getConcurrent());
                CountDownLatch latch = new CountDownLatch(
                        (int) (request.getEndMessageId() - request.getStartMessageId() + 1));

                // 使用原子计数器来避免lambda表达式的final变量问题
                AtomicInteger successCounter = new AtomicInteger(0);
                AtomicInteger failedCounter = new AtomicInteger(0);

                // 按消息ID范围逐个处理
                for (long messageId = request.getStartMessageId();
                     messageId <= request.getEndMessageId(); messageId++) {

                    try {
                        semaphore.acquire();

                        // 组合Telegram链接
                        String telegramLink = String.format("https://t.me/%s/%d",
                                request.getChatId().trim(), messageId);

                        // 异步处理单个链接
                        processSingleLinkForBatch(telegramLink, semaphore, latch, request.getMinDurationMinutes(),
                                () -> successCounter.incrementAndGet(),
                                () -> failedCounter.incrementAndGet());

                        // 控制请求间隔
                        if (request.getInterval() > 0 && messageId < request.getEndMessageId()) {
                            Thread.sleep(request.getInterval());
                        }

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.error("批量下载被中断", e);
                        break;
                    } catch (Exception e) {
                        log.error("处理消息ID {} 时发生错误", messageId, e);
                        failedCounter.incrementAndGet();
                        latch.countDown();
                    }
                }

                // 等待所有任务完成
                try {
                    if (latch.await(30, TimeUnit.MINUTES)) { // 最多等待30分钟
                        log.info("批量下载任务完成（链接方式）: 总数={}, 成功={}, 失败={}",
                                request.getEndMessageId() - request.getStartMessageId() + 1,
                                successCounter.get(), failedCounter.get());
                    } else {
                        log.warn("批量下载任务超时，可能部分任务仍在进行中");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("等待批量下载完成时被中断", e);
                }

            } catch (Exception e) {
                log.error("批量下载任务执行失败", e);
            }
        }).start();
    }

    /**
     * 处理单个链接（用于批量下载）
     */
    private void processSingleLinkForBatch(String link, Semaphore semaphore,
                                           CountDownLatch latch, Integer minDurationMinutes, Runnable onSuccess, Runnable onFailure) {

        Tmd.client.send(new TdApi.GetMessageLinkInfo(link), res -> {
            try {
                if (res != null && !res.isError()) {
                    TdApi.MessageLinkInfo linkInfo = res.get();
                    if (linkInfo != null && linkInfo.message != null) {
                        long messageId = linkInfo.message.id;

                        // 检查消息内容类型
                        if (linkInfo.message.content instanceof TdApi.MessageVideo video) {
                            // 检查视频时长是否满足要求（使用传入的minDurationMinutes参数）
                            int effectiveMinDuration = minDurationMinutes != null ? minDurationMinutes : 10;
                            if (video.video.duration >= effectiveMinDuration * 60) {
                                // 规范化chatId格式
                                log.info("发现视频链接 {}: {}, 原始Chat ID: {}",
                                       link, video.video.fileName, linkInfo.chatId);
                                // 复用现有的视频处理逻辑，使用规范化后的chatId
                                fun.zulin.tmd.telegram.handler.UpdateNewMessageHandler.processVideoMessage(
                                        messageId, video, linkInfo.chatId);
                                onSuccess.run();
                            } else {
                                log.info("视频链接时长不足{}分钟，跳过下载: {} (时长: {}秒)", 
                                        effectiveMinDuration, video.video.fileName, video.video.duration);
                                onSuccess.run(); // 仍然标记为成功，因为这是预期行为
                                latch.countDown();
                            }
                        } else if (linkInfo.message.content instanceof TdApi.MessageDocument document) {
                            log.info("发现文档链接 {}: {}", link, document.document.fileName);
                            // 处理文档消息，并传递latch用于真正的完成通知
                            processDocumentMessageWithLatch(messageId, document, latch, onSuccess, onFailure);
                        } else if (linkInfo.message.content instanceof TdApi.MessagePhoto photo) {
                            log.info("发现图片链接 {}: {}", link, "photo");
                            // 处理图片消息，并传递latch用于真正的完成通知
                            processPhotoMessageWithLatch(messageId, photo, latch, onSuccess, onFailure);
                        } else {
                            log.warn("链接 {} 指向的消息类型不支持下载: {}", link,
                                    linkInfo.message.content.getClass().getSimpleName());
                            onFailure.run();
                            latch.countDown(); // 只有在失败时才减少计数器
                        }
                    } else {
                        log.warn("无法解析链接信息: {}", link);
                        onFailure.run();
                        latch.countDown(); // 只有在失败时才减少计数器
                    }
                } else {
                    log.error("解析Telegram链接失败: {}", link);
                    onFailure.run();
                    latch.countDown(); // 只有在失败时才减少计数器
                }

            } catch (Exception e) {
                log.error("处理链接 {} 时发生异常", link, e);
                onFailure.run();
                latch.countDown(); // 只有在失败时才减少计数器
            } finally {
                semaphore.release();
                // 注意：这里不再调用latch.countDown()，由具体的处理方法负责
            }
        });
    }

    /**
     * 处理文档消息（带回调通知）
     */
    private void processDocumentMessageWithLatch(long messageId, TdApi.MessageDocument document,
                                                 CountDownLatch latch, Runnable onSuccess, Runnable onFailure) {
        try {
            var uniqueId = document.document.document.remote.uniqueId;
            String originalFilename = document.document.fileName;
            String captionText = document.caption != null ? document.caption.text : null;

            // 使用公共方法处理下载
            processDownloadItem(uniqueId, originalFilename, captionText, 
                    document.document.document.id, document.document.document.size,
                    "Document", "开始下载文档: {}", latch, onSuccess, onFailure);

        } catch (Exception e) {
            log.error("处理文档消息 {} 失败", messageId, e);
            onFailure.run();
            latch.countDown();
        }
    }

    /**
     * 处理图片消息（带回调通知）
     */
    private void processPhotoMessageWithLatch(long messageId, TdApi.MessagePhoto photo,
                                             CountDownLatch latch, Runnable onSuccess, Runnable onFailure) {
        try {
            // 获取最大的图片尺寸
            TdApi.PhotoSize largestSize = findLargestPhotoSize(photo.photo.sizes);

            if (largestSize == null) {
                log.warn("图片消息 {} 没有可用的尺寸", messageId);
                onFailure.run();
                latch.countDown();
                return;
            }

            var uniqueId = largestSize.photo.remote.uniqueId;
            String captionText = photo.caption != null ? photo.caption.text : null;

            // 使用公共方法处理下载
            processDownloadItem(uniqueId, "photo.jpg", captionText,
                    largestSize.photo.id, largestSize.photo.size,
                    "Photo", "开始下载图片: {}", latch, onSuccess, onFailure);

        } catch (Exception e) {
            log.error("处理图片消息 {} 失败", messageId, e);
            onFailure.run();
            latch.countDown();
        }
    }

    /**
     * 查找最大的图片尺寸
     */
    private TdApi.PhotoSize findLargestPhotoSize(TdApi.PhotoSize[] sizes) {
        if (sizes == null || sizes.length == 0) {
            return null;
        }
        TdApi.PhotoSize largest = null;
        for (TdApi.PhotoSize size : sizes) {
            if (largest == null || size.photo.size > largest.photo.size) {
                largest = size;
            }
        }
        return largest;
    }

    /**
     * 公共下载处理方法
     * 统一处理文档、图片等下载项的创建、保存和下载流程
     */
    private void processDownloadItem(String uniqueId, String originalFilename, String captionText,
                                      int fileId, long fileSize, String fileType,
                                      String startLogMessage, CountDownLatch latch, 
                                      Runnable onSuccess, Runnable onFailure) {
        var service = SpringContext.getBean(DownloadItemServiceImpl.class);

        // 检查是否已存在
        var existingItem = service.getByUniqueId(uniqueId);
        if (existingItem != null) {
            log.info("{} {} 已存在，跳过下载", fileType, originalFilename);
            onSuccess.run();
            latch.countDown();
            return;
        }

        // 构造描述
        String description = buildDescription(captionText, originalFilename, fileType);

        // 保存到数据库
        DownloadItem item = DownloadItem.builder()
                .description(description)
                .filename("temp_placeholder")
                .caption(captionText)
                .createTime(LocalDateTime.now(ZoneId.of("Asia/Shanghai")))
                .downloadedSize(0)
                .fileId(fileId)
                .fileSize(fileSize)
                .massageId(0L) // 将在回调中设置
                .chatId(Tmd.savedMessagesChat.id)
                .uniqueId(uniqueId)
                .state(DownloadState.Created.name())
                .build();

        service.save(item);

        // 使用数据库ID重命名
        String extension = getFileExtension(originalFilename);
        String idBasedFilename = item.getId() + extension;
        item.setFilename(idBasedFilename);
        service.updateById(item);

        // 添加到下载队列
        DownloadManage.addDownloadingItems(item);

        // 注册下载完成回调
        registerDownloadCompletionCallback(item, latch, onSuccess, onFailure);

        // 开始下载
        DownloadManage.download(item);
        log.info(startLogMessage, idBasedFilename);
    }

    /**
     * 构造文件描述
     */
    private String buildDescription(String captionText, String filename, String defaultName) {
        StringBuilder desc = new StringBuilder();
        if (captionText != null && !captionText.trim().isEmpty()) {
            desc.append(captionText.trim());
        }
        if (filename != null && !filename.trim().isEmpty() && !filename.equals("photo.jpg")) {
            if (desc.length() > 0) {
                desc.append(" - ");
            }
            desc.append(filename.trim());
        }
        if (desc.length() == 0) {
            desc.append("Unnamed ").append(defaultName);
        }
        return desc.toString();
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return ".jpg";
        }
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot) : ".jpg";
    }





    /**
     * 注册下载完成回调
     */
    private void registerDownloadCompletionCallback(DownloadItem item, CountDownLatch latch,
                                                    Runnable onSuccess, Runnable onFailure) {
        // 创建一个监听器来监控下载状态变化
        DownloadManage.StateChangeListener listener = new DownloadManage.StateChangeListener() {
            @Override
            public void onStateChanged(DownloadItem updatedItem, String oldState, String newState) {
                if (updatedItem.getUniqueId().equals(item.getUniqueId())) {
                    if (DownloadState.Complete.name().equals(newState)) {
                        log.info("下载完成: {}", updatedItem.getFilename());
                        onSuccess.run();
                        latch.countDown();
                        // 移除监听器
                        DownloadManage.removeListener(this);
                    } else if (DownloadState.Failed.name().equals(newState)) {
                        log.error("下载失败: {}", updatedItem.getFilename());
                        onFailure.run();
                        latch.countDown();
                        // 移除监听器
                        DownloadManage.removeListener(this);
                    }
                }
            }
        };

        // 添加监听器
        DownloadManage.addListener(listener);
    }

    /**
     * 通过Telegram链接下载视频
     */
    @PostMapping("/telegram-link")
    public ApiResponse<DownloadItem> downloadByTelegramLink(@RequestBody TelegramLinkRequest request) {
        try {
            String link = request.getLink();
            if (link == null || link.trim().isEmpty()) {
                return ApiResponse.error(400, "链接不能为空");
            }

            // 验证链接格式
            if (!link.toLowerCase().startsWith("https://t.me")) {
                return ApiResponse.error(400, "请输入有效的Telegram链接");
            }

            // 检查Telegram客户端是否就绪
            if (Tmd.client == null || Tmd.savedMessagesChat == null) {
                return ApiResponse.error(503, "Telegram客户端未就绪，请先登录");
            }

            log.info("收到Telegram链接下载请求: {}", link);

            // 异步处理链接解析和下载
            Tmd.client.send(new TdApi.GetMessageLinkInfo(link), res -> {
                if (res != null && !res.isError()) {
                    TdApi.MessageLinkInfo linkInfo = res.get();
                    if (linkInfo != null && linkInfo.message != null) {
                        // 检查是否为视频消息
                        if (linkInfo.message.content instanceof TdApi.MessageVideo video) {
                            log.info("开始下载链接中的视频: {}", link);
                            // 复用现有的处理逻辑
                            fun.zulin.tmd.telegram.handler.UpdateNewMessageHandler.processVideoMessage(
                                    linkInfo.message.id, video, linkInfo.chatId);
                        } else {
                            log.warn("链接指向的消息不是视频类型: {}",
                                    linkInfo.message.content.getClass().getSimpleName());
                        }
                    } else {
                        log.warn("无法解析链接信息: {}", link);
                    }
                } else {
                    log.error("解析Telegram链接失败: {}", link);
                }
            });

            // 返回成功响应（异步处理）
            return new ApiResponse<>(200, "已提交下载请求，正在处理中...", null);

        } catch (Exception e) {
            log.error("处理Telegram链接下载请求失败: {}", request.getLink(), e);
            return ApiResponse.error(500, "处理失败: " + e.getMessage());
        }
    }

    /**
     * 清理所有下载数据
     */
    @PostMapping("/clean-all")
    public ApiResponse<Void> cleanAllData() {
        try {
            var cleanupUtil = SpringContext.getBean(DataCleanupUtil.class);
            cleanupUtil.cleanAllData();
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("清理所有数据失败", e);
            return ApiResponse.error(500, "清理失败: " + e.getMessage());
        }
    }

    /**
     * 按状态清理下载数据
     */
    @PostMapping("/clean-by-status/{status}")
    public ApiResponse<Void> cleanDataByStatus(@PathVariable String status) {
        try {
            var cleanupUtil = SpringContext.getBean(DataCleanupUtil.class);
            cleanupUtil.cleanDataByStatus(status);
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("按状态清理数据失败: {}", status, e);
            return ApiResponse.error(500, "清理失败: " + e.getMessage());
        }
    }

    /**
     * 删除已下载的文件
     *
     * @param item 下载项
     * @return 删除是否成功
     */
    private boolean deleteDownloadedFile(DownloadItem item) {
        try {
            if (item.getFilename() == null || item.getFilename().isEmpty()) {
                log.warn("下载项 {} 的文件名为空", item.getUniqueId());
                return false;
            }

            // 构建文件路径
            Path filePath = Paths.get(SystemConstants.File.getVideosDirPath()).resolve(item.getFilename());

            // 检查文件是否存在
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("成功删除文件: {}", filePath.toAbsolutePath());
                return true;
            } else {
                log.warn("要删除的文件不存在: {}", filePath.toAbsolutePath());
                return true; // 文件不存在也算删除成功
            }

        } catch (IOException e) {
            log.error("删除文件失败: {} (文件名: {})", item.getUniqueId(), item.getFilename(), e);
            return false;
        } catch (Exception e) {
            log.error("删除文件过程中发生异常: {}", item.getUniqueId(), e);
            return false;
        }
    }
}

