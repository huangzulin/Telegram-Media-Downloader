package fun.zulin.tmd.utils;

import fun.zulin.tmd.data.item.DownloadItem;
import fun.zulin.tmd.data.item.DownloadItemServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 数据清理工具
 * 用于清理旧的下载数据和文件
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataCleanupUtil implements CommandLineRunner {
    
    private final DownloadItemServiceImpl downloadItemService;
    private final DownloadDirectoryManager directoryManager;

    @Override
    public void run(String... args) throws Exception {
        // 检查是否有清理参数
        for (String arg : args) {
            if ("--clean-data".equals(arg)) {
                log.info("检测到数据清理参数，开始清理所有旧数据...");
                cleanAllData();
                System.exit(0);
            }
        }
    }

    /**
     * 清理所有下载数据
     */
    public void cleanAllData() {
        try {
            log.info("开始清理所有下载数据...");
            
            // 1. 删除所有下载记录
            deleteAllDownloadRecords();
            
            // 2. 删除下载文件
            deleteDownloadFiles();
            
            // 3. 删除缩略图文件
            deleteThumbnailFiles();
            
            log.info("数据清理完成！");
            
        } catch (Exception e) {
            log.error("数据清理过程中发生错误", e);
            throw new RuntimeException("数据清理失败", e);
        }
    }

    /**
     * 删除所有下载记录
     */
    private void deleteAllDownloadRecords() {
        try {
            log.info("正在删除数据库中的所有下载记录...");
            
            // 获取所有下载项
            List<DownloadItem> allItems = downloadItemService.list();
            int totalCount = allItems.size();
            
            if (totalCount > 0) {
                // 批量删除
                boolean deleted = downloadItemService.removeBatchByIds(
                    allItems.stream().map(DownloadItem::getId).toList()
                );
                
                if (deleted) {
                    log.info("成功删除 {} 条下载记录", totalCount);
                } else {
                    log.warn("删除下载记录失败");
                }
            } else {
                log.info("数据库中没有下载记录需要删除");
            }
            
        } catch (Exception e) {
            log.error("删除下载记录时发生错误", e);
            throw e;
        }
    }

    /**
     * 删除下载文件
     */
    private void deleteDownloadFiles() {
        try {
            log.info("正在删除下载文件...");
            
            Path videosPath = directoryManager.getVideosPath();
            if (Files.exists(videosPath)) {
                deleteFilesInDirectory(videosPath, "下载文件");
            } else {
                log.info("下载目录不存在: {}", videosPath);
            }
            
        } catch (Exception e) {
            log.error("删除下载文件时发生错误", e);
        }
    }

    /**
     * 删除缩略图文件
     */
    private void deleteThumbnailFiles() {
        try {
            log.info("正在删除缩略图文件...");
            
            Path thumbnailsPath = directoryManager.getThumbnailsPath();
            if (Files.exists(thumbnailsPath)) {
                deleteFilesInDirectory(thumbnailsPath, "缩略图文件");
            } else {
                log.info("缩略图目录不存在: {}", thumbnailsPath);
            }
            
        } catch (Exception e) {
            log.error("删除缩略图文件时发生错误", e);
        }
    }

    /**
     * 删除指定目录下的所有文件
     */
    private void deleteFilesInDirectory(Path directory, String fileType) {
        try {
            if (!Files.exists(directory)) {
                log.info("{}目录不存在: {}", fileType, directory);
                return;
            }
            
            // 列出目录中的所有文件
            List<Path> files = Files.list(directory)
                .filter(Files::isRegularFile)
                .toList();
            
            int fileCount = files.size();
            if (fileCount > 0) {
                log.info("找到 {} 个{}，开始删除...", fileCount, fileType);
                
                int deletedCount = 0;
                for (Path file : files) {
                    try {
                        Files.delete(file);
                        deletedCount++;
                        log.debug("已删除文件: {}", file.getFileName());
                    } catch (IOException e) {
                        log.warn("删除文件失败: {} - {}", file.getFileName(), e.getMessage());
                    }
                }
                
                log.info("成功删除 {} 个{}中的 {} 个", fileCount, fileType, deletedCount);
            } else {
                log.info("{}目录为空", fileType);
            }
            
        } catch (Exception e) {
            log.error("处理{}目录时发生错误", fileType, e);
        }
    }

    /**
     * 清理特定状态的下载数据
     * @param status 要清理的状态 ('Created', 'Downloading', 'Complete', 'Failed')
     */
    public void cleanDataByStatus(String status) {
        try {
            log.info("开始清理状态为 {} 的下载数据...", status);
            
            // 查询指定状态的下载项
            List<DownloadItem> items = downloadItemService.lambdaQuery()
                .eq(DownloadItem::getState, status)
                .list();
            
            int count = items.size();
            if (count > 0) {
                log.info("找到 {} 个状态为 {} 的下载项", count, status);
                
                // 删除对应的文件
                int fileDeleted = 0;
                for (DownloadItem item : items) {
                    if (deleteDownloadFile(item)) {
                        fileDeleted++;
                    }
                }
                
                // 删除数据库记录
                boolean recordsDeleted = downloadItemService.removeBatchByIds(
                    items.stream().map(DownloadItem::getId).toList()
                );
                
                if (recordsDeleted) {
                    log.info("成功清理 {} 个{}状态的下载项，删除了 {} 个文件", 
                        count, status, fileDeleted);
                }
            } else {
                log.info("没有找到状态为 {} 的下载项", status);
            }
            
        } catch (Exception e) {
            log.error("按状态清理数据时发生错误", e);
            throw new RuntimeException("清理失败", e);
        }
    }

    /**
     * 删除单个下载项对应的文件
     */
    private boolean deleteDownloadFile(DownloadItem item) {
        try {
            if (item.getFilename() != null && !item.getFilename().isEmpty()) {
                Path filePath = directoryManager.getVideosPath().resolve(item.getFilename());
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    log.debug("删除文件: {}", item.getFilename());
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.warn("删除文件 {} 失败: {}", item.getFilename(), e.getMessage());
            return false;
        }
    }
}