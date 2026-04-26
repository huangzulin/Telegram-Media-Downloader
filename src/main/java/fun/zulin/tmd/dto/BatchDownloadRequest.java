package fun.zulin.tmd.dto;

import lombok.Data;

/**
 * 批量下载请求DTO
 */
@Data
public class BatchDownloadRequest {
    
    /**
     * 频道/群组ID或用户名
     */
    private String chatId;
    
    /**
     * 起始消息ID
     */
    private Long startMessageId;
    
    /**
     * 结束消息ID
     */
    private Long endMessageId;
    
    /**
     * 并发数（1-10）
     */
    private Integer concurrent = 3;
    
    /**
     * 请求间隔（毫秒，0-5000）
     */
    private Integer interval = 1000;
    
    /**
     * 最小时长（分钟）
     */
    private Integer minDurationMinutes = 10;
}
