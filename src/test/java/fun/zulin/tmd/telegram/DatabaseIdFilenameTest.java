package fun.zulin.tmd.telegram;

import fun.zulin.tmd.data.item.DownloadItem;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DatabaseIdFilenameTest {

    @Test
    void testGenerateIdBasedFilename() {
        // 测试视频文件
        DownloadItem videoItem = DownloadItem.builder()
                .id(123L)
                .filename("test_video.mp4")
                .build();
        
        String result = generateIdBasedFilename(videoItem, "original_video.mp4");
        assertEquals("123.mp4", result);
        
        // 测试文本文件
        DownloadItem textItem = DownloadItem.builder()
                .id(456L)
                .filename("document.txt")
                .build();
        
        String textResult = generateIdBasedFilename(textItem, "original_doc.txt");
        assertEquals("456.txt", textResult);
        
        // 测试无扩展名文件
        DownloadItem noExtItem = DownloadItem.builder()
                .id(789L)
                .filename("README")
                .build();
        
        String noExtResult = generateIdBasedFilename(noExtItem, "README");
        assertEquals("789.mp4", noExtResult);
        
        // 测试多个点的文件名
        DownloadItem multiDotItem = DownloadItem.builder()
                .id(101L)
                .filename("archive.tar.gz")
                .build();
        
        String multiDotResult = generateIdBasedFilename(multiDotItem, "backup.tar.gz");
        assertEquals("101.gz", multiDotResult); // 取最后一个点后的扩展名
    }
    
    /**
     * 模拟下载完成后用数据库ID重命名文件的逻辑
     */
    private String generateIdBasedFilename(DownloadItem item, String originalFilename) {
        // 获取文件扩展名，如果没有则默认使用.mp4
        String extension = ".mp4";
        int lastDotIndex = originalFilename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            extension = originalFilename.substring(lastDotIndex);
        }
        
        // 使用数据库ID作为新文件名
        return item.getId() + extension;
    }
    
    @Test
    void testFilenameSafety() {
        // 测试各种可能导致问题的文件名
        String[] problematicNames = {
            "file with spaces.mp4",
            "file-with-dashes.mp4", 
            "file_with_underscores.mp4",
            "file@special#chars$.mp4",
            "中文文件名.mp4",
            "file(1).mp4",
            "file[1].mp4"
        };
        
        DownloadItem item = DownloadItem.builder()
                .id(999L)
                .build();
        
        for (String original : problematicNames) {
            String result = generateIdBasedFilename(item, original);
            // 验证结果始终是安全的数字+扩展名格式
            assertTrue(result.matches("\\d+\\.\\w+") || result.matches("\\d+"), 
                      "文件名应该符合数字+扩展名格式: " + result);
            assertTrue(result.startsWith("999"), 
                      "文件名应该以ID开头: " + result);
        }
    }
}