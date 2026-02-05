package fun.zulin.tmd.controller;

import fun.zulin.tmd.common.exception.ApiResponse;
import fun.zulin.tmd.utils.DownloadDirectoryManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 下载目录状态API控制器
 * 提供目录健康状态查询和管理功能
 */
@Slf4j
@RestController
@RequestMapping("/api/directory")
@RequiredArgsConstructor
public class DirectoryStatusController {
    
    private final DownloadDirectoryManager directoryManager;
    
    /**
     * 获取目录状态信息
     */
    @GetMapping("/status")
    public ApiResponse<DownloadDirectoryManager.DirectoryStatus> getDirectoryStatus() {
        try {
            DownloadDirectoryManager.DirectoryStatus status = directoryManager.getStatus();
            return ApiResponse.success(status);
        } catch (Exception e) {
            log.error("获取目录状态失败", e);
            return ApiResponse.error(500, "获取目录状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 手动检查目录可用性
     */
    @PostMapping("/check")
    public ApiResponse<Map<String, Object>> checkDirectory() {
        try {
            DownloadDirectoryManager.DirectoryStatus status = directoryManager.getStatus();
            Map<String, Object> result = new HashMap<>();
            result.put("available", status.isAvailable());
            result.put("path", status.getPath());
            result.put("message", status.isAvailable() ? 
                "目录可访问" : "目录不可访问，请检查U盘或移动硬盘连接");
            
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("检查目录可用性失败", e);
            return ApiResponse.error(500, "检查目录可用性失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取目录使用统计信息
     */
    @GetMapping("/usage")
    public ApiResponse<Map<String, Object>> getDirectoryUsage() {
        try {
            DownloadDirectoryManager.DirectoryStatus status = directoryManager.getStatus();
            
            if (!status.isAvailable()) {
                return ApiResponse.error(503, "目录不可用，无法获取使用信息");
            }
            
            Map<String, Object> usageInfo = new HashMap<>();
            usageInfo.put("path", status.getPath());
            
            // 计算各子目录的使用情况
            usageInfo.put("videos", calculateDirectoryUsage(status.getVideosPath()));
            usageInfo.put("thumbnails", calculateDirectoryUsage(status.getThumbnailsPath()));
            usageInfo.put("temp", calculateDirectoryUsage(status.getTempPath()));
            
            return ApiResponse.success(usageInfo);
        } catch (Exception e) {
            log.error("获取目录使用信息失败", e);
            return ApiResponse.error(500, "获取目录使用信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 计算目录使用情况
     */
    private Map<String, Object> calculateDirectoryUsage(String pathStr) {
        Map<String, Object> usage = new HashMap<>();
        try {
            java.nio.file.Path path = java.nio.file.Path.of(pathStr);
            
            if (java.nio.file.Files.exists(path)) {
                long size = calculateDirectorySize(path);
                long fileCount = countFiles(path);
                
                usage.put("exists", true);
                usage.put("size", size);
                usage.put("sizeFormatted", formatBytes(size));
                usage.put("fileCount", fileCount);
            } else {
                usage.put("exists", false);
                usage.put("size", 0L);
                usage.put("sizeFormatted", "0 B");
                usage.put("fileCount", 0L);
            }
        } catch (Exception e) {
            usage.put("exists", false);
            usage.put("error", e.getMessage());
        }
        return usage;
    }
    
    /**
     * 计算目录总大小
     */
    private long calculateDirectorySize(java.nio.file.Path directory) throws java.io.IOException {
        if (!java.nio.file.Files.exists(directory)) {
            return 0L;
        }
        
        try {
            return java.nio.file.Files.walk(directory)
                .filter(java.nio.file.Files::isRegularFile)
                .mapToLong(path -> {
                    try {
                        return java.nio.file.Files.size(path);
                    } catch (java.io.IOException e) {
                        return 0L;
                    }
                })
                .sum();
        } catch (java.io.IOException e) {
            log.warn("计算目录大小失败: {}", directory, e);
            return 0L;
        }
    }
    
    /**
     * 统计目录中文件数量
     */
    private long countFiles(java.nio.file.Path directory) throws java.io.IOException {
        if (!java.nio.file.Files.exists(directory)) {
            return 0L;
        }
        
        try {
            return java.nio.file.Files.walk(directory)
                .filter(java.nio.file.Files::isRegularFile)
                .count();
        } catch (java.io.IOException e) {
            log.warn("统计文件数量失败: {}", directory, e);
            return 0L;
        }
    }
    
    /**
     * 格式化字节数
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}