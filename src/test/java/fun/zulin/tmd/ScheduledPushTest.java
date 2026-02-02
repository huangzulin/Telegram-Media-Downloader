package fun.zulin.tmd.controller;

import fun.zulin.tmd.common.constant.SystemConstants;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ScheduledPushTest {

    @Test
    void testConstantsValues() {
        // 测试WebSocket常量
        assertEquals("/topic/auth", SystemConstants.WebSocket.AUTH_TOPIC);
        assertEquals("/topic/downloaded", SystemConstants.WebSocket.DOWNLOADED_TOPIC);
        assertEquals("/topic/downloading", SystemConstants.WebSocket.DOWNLOADING_TOPIC);
        
        // 测试调度常量
        assertEquals(2000L, SystemConstants.Schedule.DOWNLOADED_PUSH_INTERVAL);
        assertEquals(500L, SystemConstants.Schedule.DOWNLOADING_PUSH_INTERVAL);
        
        // 测试下载常量
        assertEquals(16, SystemConstants.Download.DEFAULT_PRIORITY);
        assertEquals(20, SystemConstants.Download.DOWNLOAD_TIMEOUT_MINUTES);
        assertEquals(5, SystemConstants.Download.PROGRESS_UPDATE_COUNT);
    }
}