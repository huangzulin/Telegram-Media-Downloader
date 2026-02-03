package fun.zulin.tmd.telegram.handler;

import fun.zulin.tmd.data.item.DownloadItem;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultExtensionTest {

    @Test
    void testDefaultMp4Extension() {
        // 测试没有扩展名的文件名
        DownloadItem item1 = DownloadItem.builder().id(123L).build();
        String result1 = generateIdBasedFilename(item1, "README");
        assertEquals("123.mp4", result1, "无扩展名文件应默认使用.mp4");
        
        // 测试空文件名
        DownloadItem item2 = DownloadItem.builder().id(456L).build();
        String result2 = generateIdBasedFilename(item2, "");
        assertEquals("456.mp4", result2, "空文件名应默认使用.mp4");
        
        // 测试null文件名
        DownloadItem item3 = DownloadItem.builder().id(789L).build();
        String result3 = generateIdBasedFilename(item3, null);
        assertEquals("789.mp4", result3, "null文件名应默认使用.mp4");
        
        // 测试只有点的文件名
        DownloadItem item4 = DownloadItem.builder().id(101L).build();
        String result4 = generateIdBasedFilename(item4, ".");
        assertEquals("101.mp4", result4, "只有点的文件名应默认使用.mp4");
        
        System.out.println("✓ 默认.mp4扩展名测试通过");
    }
    
    @Test
    void testPreserveExistingExtensions() {
        // 测试保留现有的扩展名
        DownloadItem item1 = DownloadItem.builder().id(202L).build();
        String result1 = generateIdBasedFilename(item1, "video.mp4");
        assertEquals("202.mp4", result1, "应保留.mp4扩展名");
        
        DownloadItem item2 = DownloadItem.builder().id(303L).build();
        String result2 = generateIdBasedFilename(item2, "document.txt");
        assertEquals("303.txt", result2, "应保留.txt扩展名");
        
        DownloadItem item3 = DownloadItem.builder().id(404L).build();
        String result3 = generateIdBasedFilename(item3, "archive.tar.gz");
        assertEquals("404.gz", result3, "应取最后一个扩展名.gz");
        
        System.out.println("✓ 现有扩展名保留测试通过");
    }
    
    /**
     * 模拟UpdateNewMessageHandler中的文件名生成逻辑
     */
    private String generateIdBasedFilename(DownloadItem item, String originalFilename) {
        // 获取文件扩展名
        String extension = ".mp4"; // 默认扩展名
        if (originalFilename != null && !originalFilename.trim().isEmpty()) {
            int lastDotIndex = originalFilename.lastIndexOf('.');
            if (lastDotIndex > 0) {
                extension = originalFilename.substring(lastDotIndex);
            }
        }
        
        // 使用数据库ID作为文件名
        return item.getId() + extension;
    }
}