package fun.zulin.tmd;

import fun.zulin.tmd.common.exception.ApiResponse;
import fun.zulin.tmd.config.DatabaseInitializer;
import fun.zulin.tmd.data.item.DownloadItem;
import fun.zulin.tmd.data.item.DownloadItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
public class DebugController {
    
    private final DownloadItemService downloadItemService;
    private final DatabaseInitializer databaseInitializer;
    
    @GetMapping("/database-files")
    public ApiResponse<List<DownloadItem>> getDatabaseFiles() {
        try {
            List<DownloadItem> items = downloadItemService.getDownloadedItem();
            log.info("数据库中找到 {} 个已完成文件", items.size());
            items.forEach(item -> log.info("文件: {} (状态: {})", item.getFilename(), item.getState()));
            return ApiResponse.success(items);
        } catch (Exception e) {
            log.error("查询数据库文件失败", e);
            return ApiResponse.error(500, "查询失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/filesystem-files")
    public ApiResponse<List<String>> getFilesystemFiles() {
        try {
            Path downloadsPath = Paths.get("downloads");
            if (!Files.exists(downloadsPath)) {
                return ApiResponse.success(List.of("downloads目录不存在"));
            }
            
            List<String> allFiles = Files.walk(downloadsPath)
                .filter(Files::isRegularFile)
                .map(path -> downloadsPath.relativize(path).toString())
                .collect(Collectors.toList());
            
            log.info("文件系统中找到 {} 个文件", allFiles.size());
            allFiles.forEach(file -> log.info("文件路径: {}", file));
            return ApiResponse.success(allFiles);
        } catch (IOException e) {
            log.error("遍历文件系统失败", e);
            return ApiResponse.error(500, "遍历失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/find-video/{encodedFilename}")
    public ApiResponse<Map<String, Object>> findVideoFile(@PathVariable String encodedFilename) {
        try {
            String filename = java.net.URLDecoder.decode(encodedFilename, "UTF-8");
            Path videosPath = Paths.get("downloads/videos");
            
            Map<String, Object> result = new HashMap<>();
            result.put("requestedFilename", filename);
            result.put("encodedFilename", encodedFilename);
            
            if (!Files.exists(videosPath)) {
                result.put("error", "videos目录不存在");
                return ApiResponse.success(result);
            }
            
            // 查找完全匹配的文件
            List<Path> matchingFiles = Files.walk(videosPath)
                .filter(Files::isRegularFile)
                .filter(path -> {
                    String fileName = path.getFileName().toString();
                    return fileName.equals(filename);
                })
                .collect(Collectors.toList());
            
            result.put("exactMatches", matchingFiles.stream()
                .map(path -> videosPath.relativize(path).toString())
                .collect(Collectors.toList()));
            
            // 查找包含关键字的文件
            List<Path> partialMatches = Files.walk(videosPath)
                .filter(Files::isRegularFile)
                .filter(path -> {
                    String fileName = path.getFileName().toString();
                    // 提取文件名中的关键词进行模糊匹配
                    return containsSimilarKeywords(fileName, filename);
                })
                .collect(Collectors.toList());
            
            result.put("partialMatches", partialMatches.stream()
                .map(path -> videosPath.relativize(path).toString())
                .collect(Collectors.toList()));
            
            // 列出所有文件供参考
            List<String> allFiles = Files.walk(videosPath)
                .filter(Files::isRegularFile)
                .map(path -> path.getFileName().toString())
                .collect(Collectors.toList());
            result.put("allVideoFiles", allFiles);
            
            log.info("查找文件 '{}' 的结果: exact={}, partial={}", 
                    filename, matchingFiles.size(), partialMatches.size());
            
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("查找视频文件失败", e);
            return ApiResponse.error(500, "查找失败: " + e.getMessage());
        }
    }
    
    private boolean containsSimilarKeywords(String fileName, String targetName) {
        // 移除特殊符号进行比较
        String cleanFileName = fileName.replaceAll("[\\【\\】，\\,\\s]+", "");
        String cleanTargetName = targetName.replaceAll("[\\【\\】，\\,\\s]+", "");
        
        // 检查是否包含相同的关键字
        return cleanFileName.contains(cleanTargetName) || 
               cleanTargetName.contains(cleanFileName) ||
               calculateSimilarity(cleanFileName, cleanTargetName) > 0.8;
    }
    
    private double calculateSimilarity(String s1, String s2) {
        if (s1.equals(s2)) return 1.0;
        if (s1.isEmpty() || s2.isEmpty()) return 0.0;
        
        // 简单的相似度计算（基于最长公共子序列）
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }
        
        return (double) dp[s1.length()][s2.length()] / Math.max(s1.length(), s2.length());
    }
    
    /**
     * 手动触发数据库迁移
     */
    @PostMapping("/migrate-database")
    public ApiResponse<String> migrateDatabase() {
        try {
            log.info("手动触发数据库迁移");
            databaseInitializer.run();
            return ApiResponse.success("数据库迁移完成");
        } catch (Exception e) {
            log.error("数据库迁移失败", e);
            return ApiResponse.error(500, "迁移失败: " + e.getMessage());
        }
    }
    
    /**
     * 检查缩略图相关数据
     */
    @GetMapping("/thumbnails-info")
    public ApiResponse<Map<String, Object>> getThumbnailsInfo() {
        try {
            log.info("检查缩略图相关信息");
            
            Map<String, Object> result = new HashMap<>();
            
            // 检查数据库中的缩略图信息
            List<DownloadItem> items = downloadItemService.getDownloadedItem();
            List<DownloadItem> itemsWithThumbnails = items.stream()
                .filter(item -> item.getThumbnail() != null && !item.getThumbnail().isEmpty())
                .collect(Collectors.toList());
            

            
            result.put("totalItems", items.size());
            result.put("itemsWithThumbnails", itemsWithThumbnails.size());
            
            // 显示缩略图详情
            List<Map<String, Object>> thumbnailDetails = itemsWithThumbnails.stream()
                .map(item -> {
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("id", item.getId());
                    detail.put("filename", item.getFilename());
                    detail.put("thumbnail", item.getThumbnail());
                    detail.put("state", item.getState());
                    return detail;
                })
                .collect(Collectors.toList());
            
            result.put("thumbnailDetails", thumbnailDetails);
            
            // 检查缩略图文件系统
            Path thumbnailsDir = Paths.get("downloads/thumbnails");
            if (Files.exists(thumbnailsDir)) {
                List<String> thumbnailFiles = Files.walk(thumbnailsDir)
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.toList());
                result.put("thumbnailFilesInFs", thumbnailFiles);
                result.put("thumbnailFilesCount", thumbnailFiles.size());
            } else {
                result.put("thumbnailFilesInFs", List.of());
                result.put("thumbnailFilesCount", 0);
                result.put("thumbnailsDirExists", false);
            }
            
            log.info("缩略图信息检查完成: 总数={}, 有缩略图={}, 文件系统中={}", 
                    items.size(), itemsWithThumbnails.size(), 
                    result.get("thumbnailFilesCount"));
            
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("检查缩略图信息失败", e);
            return ApiResponse.error(500, "检查失败: " + e.getMessage());
        }
    }
    
    /**
     * 测试缩略图下载功能
     */
    @PostMapping("/test-thumbnail-download/{itemId}")
    public ApiResponse<Map<String, Object>> testThumbnailDownload(@PathVariable Long itemId) {
        try {
            log.info("测试缩略图下载功能，itemId: {}", itemId);
            
            DownloadItem item = downloadItemService.getById(itemId);
            if (item == null) {
                return ApiResponse.error(404, "找不到指定的下载项");
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("itemId", itemId);
            result.put("filename", item.getFilename());
            result.put("currentThumbnail", item.getThumbnail());
            
            // 这里可以调用实际的下载逻辑进行测试
            result.put("testStatus", "功能待实现");
            
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("测试缩略图下载失败", e);
            return ApiResponse.error(500, "测试失败: " + e.getMessage());
        }
    }
}