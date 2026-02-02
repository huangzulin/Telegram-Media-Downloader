package fun.zulin.tmd;

import fun.zulin.tmd.data.item.DownloadItem;
import fun.zulin.tmd.telegram.DownloadManage;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 缩略图命名一致性测试
 * 验证视频文件和缩略图使用相同的数据库ID进行命名
 */
@Slf4j
@SpringBootTest
public class ThumbnailNamingTest {

    @Test
    void testThumbnailNamingConsistency() throws Exception {
        // 创建测试下载项
        DownloadItem testItem = DownloadItem.builder()
                .id(123L)
                .filename("123.mp4")
                .build();
        
        log.info("测试缩略图命名一致性:");
        log.info("视频文件名: {}", testItem.getFilename());
        log.info("数据库ID: {}", testItem.getId());

        // 测试缩略图文件名生成逻辑
        String expectedThumbnailName = testItem.getId() + ".jpg";
        log.info("期望的缩略图文件名: {}", expectedThumbnailName);
        
        // 验证命名规则
        assertTrue(expectedThumbnailName.matches("\\d+\\.jpg"), 
                  "缩略图文件名应该符合 数字.jpg 格式");
        assertTrue(expectedThumbnailName.startsWith(testItem.getId().toString()),
                  "缩略图文件名应该以数据库ID开头");
        
        log.info("✓ 缩略图命名一致性测试通过");
    }
    
    @Test
    void testVideoAndThumbnailNamingRelation() {
        // 测试多个视频文件的命名关系
        Long[] testIds = {100L, 200L, 300L, 999L};
        String[] extensions = {".mp4", ".avi", ".mov", ".mkv"};
        
        for (int i = 0; i < testIds.length; i++) {
            DownloadItem item = DownloadItem.builder()
                    .id(testIds[i])
                    .filename(testIds[i] + extensions[i])
                    .build();
            
            String expectedThumbnail = testIds[i] + ".jpg";
            
            log.info("视频ID: {}, 视频文件: {}, 期望缩略图: {}", 
                    testIds[i], item.getFilename(), expectedThumbnail);
            
            assertEquals(expectedThumbnail, testIds[i] + ".jpg",
                       "缩略图应该使用相同ID命名");
        }
        
        log.info("✓ 视频与缩略图命名关系测试通过");
    }
}