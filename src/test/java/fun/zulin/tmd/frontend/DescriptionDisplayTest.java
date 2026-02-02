package fun.zulin.tmd.frontend;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DescriptionDisplayTest {

    @Test
    void testDescriptionPriorityDisplay() {
        // 测试description优先显示的逻辑
        String description = "极品大长腿丝袜御姐【小林的涩涩日常】SVIP1月13日新作.mp4";
        String filename = "123.mp4";
        
        // 模拟前端显示逻辑: description || filename || '未知文件'
        String displayTitle = (description != null && !description.isEmpty()) ? 
                             description : 
                             (filename != null && !filename.isEmpty() ? filename : "未知文件");
        
        assertEquals(description, displayTitle, "应该优先显示description");
    }
    
    @Test
    void testFilenameFallback() {
        // 测试当description为空时回退到filename
        String description = "";
        String filename = "456.mp4";
        
        String displayTitle = (description != null && !description.isEmpty()) ? 
                             description : 
                             (filename != null && !filename.isEmpty() ? filename : "未知文件");
        
        assertEquals(filename, displayTitle, "当description为空时应回退到filename");
    }
    
    @Test
    void testUnknownFileFallback() {
        // 测试当两个字段都为空时的默认值
        String description = "";
        String filename = "";
        
        String displayTitle = (description != null && !description.isEmpty()) ? 
                             description : 
                             (filename != null && !filename.isEmpty() ? filename : "未知文件");
        
        assertEquals("未知文件", displayTitle, "当两个字段都为空时应显示默认值");
    }
    
    @Test
    void testNullHandling() {
        // 测试null值处理
        String description = null;
        String filename = "test.mp4";
        
        String displayTitle1 = (description != null && !description.isEmpty()) ? 
                              description : 
                              (filename != null && !filename.isEmpty() ? filename : "未知文件");
        
        assertEquals("test.mp4", displayTitle1, "应该正确处理null值");
        
        String description2 = null;
        String filename2 = null;
        
        String displayTitle2 = (description2 != null && !description2.isEmpty()) ? 
                              description2 : 
                              (filename2 != null && !filename2.isEmpty() ? filename2 : "未知文件");
        
        assertEquals("未知文件", displayTitle2, "应该正确处理两个null值");
    }
}