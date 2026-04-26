package fun.zulin.tmd.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 批量下载响应DTO
 */
@Data
@AllArgsConstructor
public class BatchDownloadResponse {
    
    /**
     * 总任务数
     */
    private Integer totalCount;
    
    /**
     * 成功数量
     */
    private Integer successCount;
    
    /**
     * 失败数量
     */
    private Integer failedCount;
    
    /**
     * 响应消息
     */
    private String message;
}
