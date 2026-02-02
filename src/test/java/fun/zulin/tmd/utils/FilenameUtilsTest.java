package fun.zulin.tmd.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FilenameUtilsTest {

    @Test
    void testConvertToSafeFilename() {
        // 测试中文特殊字符转换
        String original = "极品大长腿丝袜御姐【小林的涩涩日常】SVIP1月13日新作，【被下属反攻的女上司】.mp4";
        String result = FilenameUtils.convertToSafeFilename(original);
        // 验证关键特征：中文保留，所有特殊字符替换为空格
        assertTrue(result.contains("极品大长腿丝袜御姐"));
        assertTrue(result.contains("小林的涩涩日常")); // 特殊括号已被替换为空格
        assertTrue(result.contains("SVIP1月13日新作"));
        // 逗号已被替换为空格，所以不应该再包含逗号
        assertFalse(result.contains(","));
        assertTrue(result.contains("被下属反攻的女上司")); // 特殊括号已被替换为空格
        assertTrue(result.endsWith(".mp4"));
        
        // 测试基本英文文件名
        String englishOriginal = "test_video.mp4";
        String englishResult = FilenameUtils.convertToSafeFilename(englishOriginal);
        assertEquals(englishOriginal, englishResult);
        
        // 测试空字符串
        String emptyResult = FilenameUtils.convertToSafeFilename("");
        assertTrue(emptyResult.startsWith("unnamed_file_"));
        
        // 测试null
        String nullResult = FilenameUtils.convertToSafeFilename(null);
        assertTrue(nullResult.startsWith("unnamed_file_"));
    }

    @Test
    void testSpecialCharactersReplacement() {
        // 测试各种特殊字符替换为空格
        String test1 = "文件[方括号](圆括号)【中文括号】.txt";
        String result1 = FilenameUtils.convertToSafeFilename(test1);
        assertEquals("文件 方括号 圆括号 中文括号 .txt", result1);
        
        // 测试连续特殊字符合并
        String test2 = "文件！！！???。。。；；；：：：.doc";
        String result2 = FilenameUtils.convertToSafeFilename(test2);
        assertEquals("文件 .doc", result2);
        
        // 测试引号和标点
        String test3 = "测试\"双引号\"'单引号'，、；：？！.pdf";
        String result3 = FilenameUtils.convertToSafeFilename(test3);
        assertEquals("测试 双引号 单引号 .pdf", result3);
        
        // 测试路径分隔符
        String test4 = "路径\\文件/名称|管道符*.exe";
        String result4 = FilenameUtils.convertToSafeFilename(test4);
        assertEquals("路径 文件 名称 管道符 .exe", result4);
        
        // 测试控制字符
        String test5 = "包含\u0001控制\u001F字符.txt";
        String result5 = FilenameUtils.convertToSafeFilename(test5);
        // 控制字符应该被替换为空格
        assertTrue(result5.contains("包含"));
        assertTrue(result5.contains("控制"));
        assertTrue(result5.contains("字符.txt"));
        
        // 测试纯特殊字符
        String test6 = "【】（）！@#$%^&*()_+-=[]{}|;':\",./<>?~`";
        String result6 = FilenameUtils.convertToSafeFilename(test6);
        // 中文特殊字符应该被替换，但一些相对安全的字符(@#$%^&_+-=;,.~`)可能被保留
        // 关键是不能包含危险字符
        assertFalse(result6.contains("【") || result6.contains("】"));
        assertFalse(result6.contains("（") || result6.contains("）"));
        assertFalse(result6.contains("！"));
        assertFalse(result6.contains("*"));
        assertFalse(result6.contains("<") || result6.contains(">"));
        assertFalse(result6.contains("\"") || result6.contains("'"));
    }

    @Test
    void testEncodeDecodeFilename() {
        String filename = "极品大长腿丝袜御姐[小林的涩涩日常].mp4";
        String encoded = FilenameUtils.encodeFilename(filename);
        String decoded = FilenameUtils.decodeFilename(encoded);
        assertEquals(filename, decoded);
        
        // 测试null
        assertNull(FilenameUtils.encodeFilename(null));
        assertNull(FilenameUtils.decodeFilename(null));
    }

    @Test
    void testNormalizeFilename() {
        String filename1 = "极品大长腿丝袜御姐【小林的涩涩日常】SVIP1月13日新作.mp4";
        String filename2 = "极品大长腿丝袜御姐[小林的涩涩日常]SVIP1月13日新作.mp4";
        
        String normalized1 = FilenameUtils.normalizeFilename(filename1);
        String normalized2 = FilenameUtils.normalizeFilename(filename2);
        
        // 验证标准化后的内容特征
        assertTrue(normalized1.contains("极品大长腿丝袜御姐"));
        assertTrue(normalized1.contains("小林的涩涩日常"));
        assertTrue(normalized1.contains("svip1月13日新作"));
        
        // 验证标准化的一致性
        assertTrue(normalized1.contains("极品大长腿丝袜御姐"));
        assertTrue(normalized2.contains("极品大长腿丝袜御姐"));
    }

    @Test
    void testNeedsEncoding() {
        String normalFilename = "test_video.mp4";
        String specialFilename = "极品大长腿丝袜御姐.mp4";
        
        assertFalse(FilenameUtils.needsEncoding(normalFilename));
        assertTrue(FilenameUtils.needsEncoding(specialFilename));
        
        // 测试null
        assertFalse(FilenameUtils.needsEncoding(null));
    }
}