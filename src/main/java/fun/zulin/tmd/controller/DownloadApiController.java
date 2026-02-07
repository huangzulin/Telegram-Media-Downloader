package fun.zulin.tmd.controller;

import fun.zulin.tmd.common.exception.ApiResponse;
import fun.zulin.tmd.data.item.DownloadItem;
import fun.zulin.tmd.data.item.DownloadItemService;
import fun.zulin.tmd.telegram.DownloadManage;
import fun.zulin.tmd.telegram.Tmd;
import it.tdlight.jni.TdApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
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
                                linkInfo.message.id, video);
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
     * Telegram链接请求DTO
     */
    public static class TelegramLinkRequest {
        private String link;
        
        // getters and setters
        public String getLink() {
            return link;
        }
        
        public void setLink(String link) {
            this.link = link;
        }
    }
    
    /**
     * 删除已下载的文件
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
            Path filePath = Paths.get("downloads/videos").resolve(item.getFilename());
            
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

