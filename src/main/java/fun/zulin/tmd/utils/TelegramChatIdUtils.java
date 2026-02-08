package fun.zulin.tmd.utils;

import lombok.extern.slf4j.Slf4j;

/**
 * Telegram Chat ID 工具类
 * 处理不同类型的Telegram Chat ID格式转换
 */
@Slf4j
public class TelegramChatIdUtils {

    // Telegram超级群组和频道的前缀
    private static final long SUPERGROUP_PREFIX = -100L;
    private static final long CHANNEL_PREFIX = -100L;

    /**
     * 规范化Chat ID，将Telegram的内部格式转换为标准格式
     * 
     * Telegram Chat ID规则：
     * - 普通群组: 正数，如 123456789
     * - 超级群组: -100 + 实际ID，如 -1001868938373 对应实际ID 1868938373
     * - 频道: -100 + 实际ID，如 -1001868938373 对应实际ID 1868938373
     * - 私聊: 负数但不以-100开头，如 -443064491
     *
     * @param chatId 原始Chat ID
     * @return 规范化的Chat ID
     */
    public static long normalizeChatId(long chatId) {
        if (chatId >= 0) {
            // 正数ID，已经是标准格式（普通群组或用户）
            log.debug("[🐛 DEBUG] Chat ID {} 已经是标准格式", chatId);
            return chatId;
        }

        // 负数ID，需要判断类型
        if (isSupergroupOrChannel(chatId)) {
            // 超级群组或频道，提取实际ID
            long actualId = extractActualId(chatId);
            log.debug("[🐛 DEBUG] 转换超级群组/频道ID: {} -> {}", chatId, actualId);
            return actualId;
        } else {
            // 其他负数ID（私聊等），保持原样
            log.debug("[🐛 DEBUG] Chat ID {} 是私聊或其他类型，保持不变", chatId);
            return chatId;
        }
    }

    /**
     * 判断是否为超级群组或频道ID
     * 
     * @param chatId 待判断的Chat ID
     * @return true表示是超级群组或频道，false表示其他类型
     */
    public static boolean isSupergroupOrChannel(long chatId) {
        return chatId < 0 && String.valueOf(Math.abs(chatId)).startsWith("100");
    }

    /**
     * 从超级群组或频道ID中提取实际ID
     * 
     * @param chatId 超级群组或频道ID（如-1001868938373）
     * @return 实际ID（如1868938373）
     */
    public static long extractActualId(long chatId) {
        if (!isSupergroupOrChannel(chatId)) {
            throw new IllegalArgumentException("Chat ID " + chatId + " 不是超级群组或频道格式");
        }
        
        String absIdStr = String.valueOf(Math.abs(chatId));
        if (absIdStr.length() <= 3) {
            throw new IllegalArgumentException("Chat ID " + chatId + " 格式不正确");
        }
        
        // 移除前缀"100"，获取实际ID
        String actualIdStr = absIdStr.substring(3);
        try {
            return Long.parseLong(actualIdStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("无法解析实际ID: " + actualIdStr, e);
        }
    }

    /**
     * 将标准ID转换回Telegram内部格式（如果需要的话）
     * 
     * @param standardId 标准ID
     * @param isSupergroup 是否为超级群组/频道
     * @return Telegram内部格式的ID
     */
    public static long toTelegramFormat(long standardId, boolean isSupergroup) {
        if (isSupergroup) {
            return SUPERGROUP_PREFIX * 10000000000L - standardId;
        } else {
            return standardId;
        }
    }

    /**
     * 获取Chat ID的类型描述
     * 
     * @param chatId Chat ID
     * @return 类型描述
     */
    public static String getChatIdType(long chatId) {
        if (chatId > 0) {
            return "普通群组或用户";
        } else if (isSupergroupOrChannel(chatId)) {
            return "超级群组或频道";
        } else {
            return "私聊";
        }
    }

    /**
     * 验证Chat ID的有效性
     * 
     * @param chatId 待验证的Chat ID
     * @return true表示有效，false表示无效
     */
    public static boolean isValidChatId(long chatId) {
        // 基本验证：不能为0
        if (chatId == 0) {
            return false;
        }
        
        // 对于超级群组/频道，验证格式
        if (isSupergroupOrChannel(chatId)) {
            try {
                long actualId = extractActualId(chatId);
                // 实际ID必须是正数且合理长度
                return actualId > 0 && String.valueOf(actualId).length() >= 10;
            } catch (IllegalArgumentException e) {
                log.warn("[🐛 DEBUG] 无效的超级群组/频道ID格式: {}", chatId);
                return false;
            }
        }
        
        // 其他类型的ID认为有效
        return true;
    }
}