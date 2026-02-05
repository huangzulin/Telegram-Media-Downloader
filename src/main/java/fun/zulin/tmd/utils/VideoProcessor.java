package fun.zulin.tmd.utils;

import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 视频处理工具类
 * 提供视频截图、格式转换等功能
 */
@Slf4j
public class VideoProcessor {
    
    private static final String THUMBNAILS_DIR = "downloads/thumbnails";
    
    /**
     * 截取视频第一帧作为封面图片
     * @param videoPath 视频文件路径
     * @return 封面图片文件名，如果失败返回null
     */
    public static String extractFirstFrameAsThumbnail(String videoPath) {
        try {
            // 确保缩略图目录存在
            Path thumbnailsDir = Paths.get(THUMBNAILS_DIR);
            if (!Files.exists(thumbnailsDir)) {
                Files.createDirectories(thumbnailsDir);
                log.info("创建缩略图目录: {}", thumbnailsDir.toAbsolutePath());
            }
            
            // 构建输入和输出路径
            Path inputPath = Paths.get(videoPath);
            if (!Files.exists(inputPath)) {
                log.warn("视频文件不存在: {}", videoPath);
                return null;
            }
            
            // 生成唯一的缩略图文件名
            String thumbnailFilename = UUID.randomUUID().toString() + ".jpg";
            Path outputPath = thumbnailsDir.resolve(thumbnailFilename);
            
            // 使用FFmpeg截取第一帧
            FFmpeg ffmpeg = new FFmpeg();
            FFprobe ffprobe = new FFprobe();
            
            FFmpegBuilder builder = new FFmpegBuilder()
                    .setInput(inputPath.toString())
                    .addOutput(outputPath.toString())
                    .setFrames(1)  // 只截取一帧
                    .done();
            
            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
            executor.createJob(builder).run();
            
            // 验证输出文件是否存在
            if (Files.exists(outputPath)) {
                log.info("成功生成视频封面: {} -> {}", 
                         inputPath.getFileName(), thumbnailFilename);
                return thumbnailFilename;
            } else {
                log.warn("封面文件生成失败: {}", outputPath);
                return null;
            }
            
        } catch (Exception e) {
            log.error("提取视频封面失败: {}", videoPath, e);
            return null;
        }
    }
    
    /**
     * 获取视频基本信息
     * @param videoPath 视频文件路径
     * @return 视频信息对象，失败返回null
     */
    public static FFmpegProbeResult getVideoInfo(String videoPath) {
        try {
            FFprobe ffprobe = new FFprobe();
            return ffprobe.probe(videoPath);
        } catch (Exception e) {
            log.error("获取视频信息失败: {}", videoPath, e);
            return null;
        }
    }
    
    /**
     * 删除指定的缩略图文件
     * @param thumbnailFilename 缩略图文件名
     * @return 删除是否成功
     */
    public static boolean deleteThumbnail(String thumbnailFilename) {
        if (thumbnailFilename == null || thumbnailFilename.isEmpty()) {
            return false;
        }
        
        try {
            Path thumbnailPath = Paths.get(THUMBNAILS_DIR, thumbnailFilename);
            if (Files.exists(thumbnailPath)) {
                Files.delete(thumbnailPath);
                log.info("删除缩略图文件: {}", thumbnailFilename);
                return true;
            }
            return false;
        } catch (IOException e) {
            log.error("删除缩略图文件失败: {}", thumbnailFilename, e);
            return false;
        }
    }
    
    /**
     * 清理所有缩略图文件
     * @return 删除的文件数量
     */
    public static int cleanupThumbnails() {
        try {
            Path thumbnailsDir = Paths.get(THUMBNAILS_DIR);
            if (!Files.exists(thumbnailsDir)) {
                return 0;
            }
            
            int deletedCount = 0;
            for (Path file : Files.list(thumbnailsDir).toArray(Path[]::new)) {
                if (Files.isRegularFile(file)) {
                    Files.delete(file);
                    deletedCount++;
                }
            }
            
            log.info("清理缩略图文件完成，共删除 {} 个文件", deletedCount);
            return deletedCount;
        } catch (Exception e) {
            log.error("清理缩略图文件失败", e);
            return 0;
        }
    }
}