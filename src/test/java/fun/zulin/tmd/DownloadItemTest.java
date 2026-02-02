package fun.zulin.tmd.telegram;

import fun.zulin.tmd.data.item.DownloadItem;
import fun.zulin.tmd.data.item.DownloadState;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DownloadManageTest {

    @Test
    void testDownloadItemCreation() {
        DownloadItem item = DownloadItem.builder()
                .filename("test.mp4")
                .fileId(12345)
                .massageId(67890L)
                .uniqueId("unique_123")
                .fileSize(1024000L)
                .downloadedSize(0L)
                .caption("Test video")
                .state(DownloadState.Created.name())
                .createTime(LocalDateTime.now())
                .build();

        assertNotNull(item);
        assertEquals("test.mp4", item.getFilename());
        assertEquals(12345, item.getFileId());
        assertEquals("unique_123", item.getUniqueId());
        assertEquals(DownloadState.Created.name(), item.getState());
    }

    @Test
    void testProgressCalculation() {
        DownloadItem item = DownloadItem.builder()
                .fileSize(1000L)
                .downloadedSize(500L)
                .build();

        Float progress = item.getProgress();
        assertEquals(50.0f, progress, 0.01);
    }

    @Test
    void testZeroFileSizeProgress() {
        DownloadItem item = DownloadItem.builder()
                .fileSize(0L)
                .downloadedSize(0L)
                .build();

        assertDoesNotThrow(() -> {
            Float progress = item.getProgress();
            assertEquals(0.0f, progress, 0.01);
        });
    }
}