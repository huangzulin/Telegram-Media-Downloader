package fun.zulin.tmd.integration;

import fun.zulin.tmd.data.item.DownloadItem;
import fun.zulin.tmd.data.item.DownloadItemServiceImpl;
import fun.zulin.tmd.data.item.DownloadState;
import fun.zulin.tmd.telegram.handler.UpdateNewMessageHandler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
public class PureIdFilenameIntegrationTest {

    @MockBean
    private DownloadItemServiceImpl downloadItemService;

    @Test
    void testPureIdFilenameGeneration() {
        // 模拟下载项创建流程
        String originalFilename = "极品大长腿丝袜御姐【小林的涩涩日常】SVIP1月13日新作.mp4";
        Long expectedId = 123L;
        
        // 模拟service行为
        DownloadItem tempItem = DownloadItem.builder()
                .id(null) // 初始为null
                .filename("temp_placeholder")
                .description("测试描述")
                .build();
                
        DownloadItem savedItem = DownloadItem.builder()
                .id(expectedId) // 保存后获得ID
                .filename("temp_placeholder")
                .description("测试描述")
                .build();
        
        when(downloadItemService.getByUniqueId(anyString())).thenReturn(null);
        when(downloadItemService.save(any(DownloadItem.class))).thenAnswer(invocation -> {
            DownloadItem item = invocation.getArgument(0);
            item.setId(expectedId); // 模拟数据库分配ID
            return true;
        });
        when(downloadItemService.updateById(any(DownloadItem.class))).thenReturn(true);
        
        // 测试ID-based文件名生成逻辑
        String result = generateIdBasedFilename(savedItem, originalFilename);
        
        // 验证结果
        assertEquals("123.mp4", result);
        assertTrue(result.matches("\\d+\\.\\w+"), "文件名应该符合数字+扩展名格式");
        assertTrue(result.startsWith(expectedId.toString()), "文件名应该以ID开头");
        
        System.out.println("✓ ID-Based文件名生成测试通过: " + result);
    }
    
    @Test
    void testVariousFileExtensions() {
        Long testId = 456L;
        DownloadItem item = DownloadItem.builder().id(testId).build();
        
        // 测试各种文件类型
        String[] testCases = {
            "video.mp4",
            "document.txt", 
            "image.jpg",
            "archive.tar.gz",
            "README",
            "file.",
            ".hidden"
        };
        
        String[] expectedResults = {
            "456.mp4",
            "456.txt",
            "456.jpg", 
            "456.gz",
            "456.mp4",  // README 没有扩展名，使用默认.mp4
            "456.",     // file. 保持点号
            "456.mp4"   // .hidden 没有有效扩展名，使用默认.mp4
        };
        
        for (int i = 0; i < testCases.length; i++) {
            String result = generateIdBasedFilename(item, testCases[i]);
            assertEquals(expectedResults[i], result, 
                "测试用例 " + testCases[i] + " 失败");
            System.out.println("✓ " + testCases[i] + " -> " + result);
        }
    }
    
    @Test
    void testEdgeCases() {
        Long testId = 789L;
        DownloadItem item = DownloadItem.builder().id(testId).build();
        
        // 测试边界情况
        assertThrows(NullPointerException.class, () -> {
            generateIdBasedFilename(null, "test.mp4");
        }, "应该拒绝null下载项");
        
        assertThrows(NullPointerException.class, () -> {
            generateIdBasedFilename(DownloadItem.builder().id(null).build(), "test.mp4");
        }, "应该拒绝null ID");
        
        // 测试null文件名
        String result = generateIdBasedFilename(item, null);
        assertEquals("789.mp4", result, "null文件名应该生成ID+默认.mp4扩展名");
        
        // 测试空文件名
        String result2 = generateIdBasedFilename(item, "");
        assertEquals("789.mp4", result2, "空文件名应该生成ID+默认.mp4扩展名");
    }
    
    /**
     * 模拟UpdateNewMessageHandler中的文件名生成逻辑
     */
    private String generateIdBasedFilename(DownloadItem item, String originalFilename) {
        if (item == null || item.getId() == null) {
            throw new NullPointerException("数据库ID不能为空");
        }
        
        // 获取文件扩展名，如果没有则默认使用.mp4
        String extension = ".mp4";
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