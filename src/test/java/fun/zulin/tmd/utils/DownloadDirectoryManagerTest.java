package fun.zulin.tmd.utils;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "DOWNLOAD_DIR=./test-downloads"
})
class DownloadDirectoryManagerTest {

    @Test
    void testDirectoryInitialization() {
        // 这个测试主要用于手动验证
        System.out.println("目录管理器测试 - 请手动验证以下功能：");
        System.out.println("1. 目录是否正确创建");
        System.out.println("2. 子目录是否正确创建");
        System.out.println("3. 权限检查是否正常工作");
        System.out.println("4. 监控功能是否启动");
    }
    
    @Test
    void testEnvironmentVariableSupport() {
        String downloadDir = System.getenv("DOWNLOAD_DIR");
        if (downloadDir != null) {
            System.out.println("环境变量 DOWNLOAD_DIR: " + downloadDir);
        } else {
            System.out.println("未设置 DOWNLOAD_DIR 环境变量，使用默认值");
        }
    }
}