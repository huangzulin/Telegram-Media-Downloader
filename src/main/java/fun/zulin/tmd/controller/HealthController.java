package fun.zulin.tmd.controller;

import fun.zulin.tmd.common.exception.ApiResponse;
import fun.zulin.tmd.telegram.DownloadManage;
import fun.zulin.tmd.telegram.Tmd;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 健康检查控制器
 */
@Slf4j
@RestController
@RequestMapping("/api")
public class HealthController {
    
    /**
     * 健康检查端点
     */
    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> healthInfo = new HashMap<>();
        
        // Telegram状态
        healthInfo.put("telegramConnected", Tmd.client != null);
        healthInfo.put("userLoggedIn", Tmd.me != null);
        if (Tmd.me != null) {
            healthInfo.put("userId", Tmd.me.id);
            // username字段可能不存在，使用替代方案
            healthInfo.put("username", "N/A");
        }
        
        // 下载状态
        healthInfo.put("activeDownloads", DownloadManage.getItems().size());
        healthInfo.put("downloadingItems", DownloadManage.getItems());
        
        // 系统信息
        healthInfo.put("timestamp", System.currentTimeMillis());
        healthInfo.put("status", (Tmd.client != null && Tmd.me != null) ? "healthy" : "unhealthy");
        
        return ApiResponse.success(healthInfo);
    }
    
    /**
     * 基本信息端点
     */
    @GetMapping("/info")
    public ApiResponse<Map<String, Object>> info() {
        Map<String, Object> info = new HashMap<>();
        info.put("app", "Telegram Media Downloader");
        info.put("version", "1.0");
        info.put("description", "Telegram媒体文件下载服务");
        return ApiResponse.success(info);
    }
    
    /**
     * 调试端点：列出downloads目录内容
     */
    @GetMapping("/debug/downloads-list")
    public ApiResponse<List<String>> listDownloads() {
        try {
            Path downloadsPath = Paths.get("downloads");
            if (!Files.exists(downloadsPath)) {
                return ApiResponse.success(Arrays.asList("downloads目录不存在"));
            }
            
            List<String> files = Files.list(downloadsPath)
                .filter(Files::isRegularFile)
                .map(Path::getFileName)
                .map(Path::toString)
                .sorted()
                .collect(Collectors.toList());
            
            log.info("downloads目录包含 {} 个文件", files.size());
            return ApiResponse.success(files);
        } catch (IOException e) {
            log.error("读取downloads目录失败", e);
            return ApiResponse.error(500, "读取目录失败: " + e.getMessage());
        }
    }
}