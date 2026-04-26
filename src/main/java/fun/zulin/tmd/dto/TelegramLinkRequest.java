package fun.zulin.tmd.dto;

import lombok.Data;

/**
 * Telegram链接请求DTO
 */
@Data
public class TelegramLinkRequest {
    
    /**
     * Telegram消息链接
     * 格式：https://t.me/chat_id/message_id
     */
    private String link;
}
