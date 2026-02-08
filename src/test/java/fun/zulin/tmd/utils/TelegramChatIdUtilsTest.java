package fun.zulin.tmd.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TelegramChatIdUtilsTest {

    @Test
    void testNormalizeChatId_Supergroup() {
        // 测试超级群组ID规范化
        long rawChatId = -1001868938373L;
        long expectedNormalizedId = 1868938373L;
        
        long result = TelegramChatIdUtils.normalizeChatId(rawChatId);
        
        assertEquals(expectedNormalizedId, result, 
            "超级群组ID应该被正确规范化");
        System.out.println("✓ 超级群组ID规范化测试通过: " + rawChatId + " -> " + result);
    }

    @Test
    void testNormalizeChatId_Channel() {
        // 测试频道ID规范化
        long rawChatId = -1001234567890L;
        long expectedNormalizedId = 1234567890L;
        
        long result = TelegramChatIdUtils.normalizeChatId(rawChatId);
        
        assertEquals(expectedNormalizedId, result, 
            "频道ID应该被正确规范化");
        System.out.println("✓ 频道ID规范化测试通过: " + rawChatId + " -> " + result);
    }

    @Test
    void testNormalizeChatId_NormalGroup() {
        // 测试普通群组ID（正数）
        long rawChatId = 123456789L;
        long expectedNormalizedId = 123456789L;
        
        long result = TelegramChatIdUtils.normalizeChatId(rawChatId);
        
        assertEquals(expectedNormalizedId, result, 
            "普通群组ID应该保持不变");
        System.out.println("✓ 普通群组ID测试通过: " + rawChatId + " -> " + result);
    }

    @Test
    void testNormalizeChatId_PrivateChat() {
        // 测试私聊ID（负数但不以-100开头）
        long rawChatId = -443064491L;
        long expectedNormalizedId = -443064491L;
        
        long result = TelegramChatIdUtils.normalizeChatId(rawChatId);
        
        assertEquals(expectedNormalizedId, result, 
            "私聊ID应该保持不变");
        System.out.println("✓ 私聊ID测试通过: " + rawChatId + " -> " + result);
    }

    @Test
    void testIsSupergroupOrChannel() {
        // 测试超级群组/频道判断
        assertTrue(TelegramChatIdUtils.isSupergroupOrChannel(-1001868938373L), 
            "以-100开头的负数应该是超级群组或频道");
        assertTrue(TelegramChatIdUtils.isSupergroupOrChannel(-1001234567890L), 
            "以-100开头的负数应该是超级群组或频道");
        
        assertFalse(TelegramChatIdUtils.isSupergroupOrChannel(123456789L), 
            "正数不应该被认为是超级群组或频道");
        assertFalse(TelegramChatIdUtils.isSupergroupOrChannel(-443064491L), 
            "不以-100开头的负数不应该被认为是超级群组或频道");
        
        System.out.println("✓ 超级群组/频道判断测试通过");
    }

    @Test
    void testExtractActualId() {
        // 测试提取实际ID
        long supergroupId = -1001868938373L;
        long expectedActualId = 1868938373L;
        
        long result = TelegramChatIdUtils.extractActualId(supergroupId);
        
        assertEquals(expectedActualId, result, 
            "应该正确提取超级群组的实际ID");
        System.out.println("✓ 实际ID提取测试通过: " + supergroupId + " -> " + result);
    }

    @Test
    void testInvalidExtractActualId() {
        // 测试无效ID提取应该抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            TelegramChatIdUtils.extractActualId(-443064491L); // 私聊ID
        }, "提取私聊ID的实际ID应该抛出异常");
        
        assertThrows(IllegalArgumentException.class, () -> {
            TelegramChatIdUtils.extractActualId(123456789L); // 普通群组ID
        }, "提取普通群组ID的实际ID应该抛出异常");
        
        System.out.println("✓ 无效ID提取异常测试通过");
    }

    @Test
    void testGetChatIdType() {
        // 测试Chat ID类型识别
        assertEquals("超级群组或频道", TelegramChatIdUtils.getChatIdType(-1001868938373L));
        assertEquals("超级群组或频道", TelegramChatIdUtils.getChatIdType(-1001234567890L));
        assertEquals("普通群组或用户", TelegramChatIdUtils.getChatIdType(123456789L));
        assertEquals("私聊", TelegramChatIdUtils.getChatIdType(-443064491L));
        
        System.out.println("✓ Chat ID类型识别测试通过");
    }

    @Test
    void testIsValidChatId() {
        // 测试Chat ID有效性验证
        assertTrue(TelegramChatIdUtils.isValidChatId(-1001868938373L), "有效的超级群组ID");
        assertTrue(TelegramChatIdUtils.isValidChatId(123456789L), "有效的普通群组ID");
        assertTrue(TelegramChatIdUtils.isValidChatId(-443064491L), "有效的私聊ID");
        
        assertFalse(TelegramChatIdUtils.isValidChatId(0L), "0不是有效的Chat ID");
        assertFalse(TelegramChatIdUtils.isValidChatId(-1001L), "格式不正确的超级群组ID");
        
        System.out.println("✓ Chat ID有效性验证测试通过");
    }

    @Test
    void testBoundaryConditions() {
        // 测试边界条件
        long minValidSupergroup = -1001000000000L; // 最小的有效超级群组ID
        long extractedMin = TelegramChatIdUtils.extractActualId(minValidSupergroup);
        assertEquals(1000000000L, extractedMin, "最小超级群组ID提取测试");
        
        long maxValidSupergroup = -1009999999999L; // 最大的有效超级群组ID
        long extractedMax = TelegramChatIdUtils.extractActualId(maxValidSupergroup);
        assertEquals(9999999999L, extractedMax, "最大超级群组ID提取测试");
        
        System.out.println("✓ 边界条件测试通过");
    }
}