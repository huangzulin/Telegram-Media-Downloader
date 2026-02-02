package fun.zulin.tmd.utils;

import org.junit.jupiter.api.Test;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

public class CharacterAnalysisTest {

    @Test
    void analyzeSpecialCharacters() {
        // 当前的特殊字符模式
        Pattern specialCharsPattern = Pattern.compile("[\\[\\](){}<>:\"/\\\\|?*\\x00-\\x1F【】（）。，；：？！‘’“”…]+");
        
        // 测试字符串
        String testInput = "测试\"双引号\"'单引号'，、；：？！.pdf";
        System.out.println("原始输入: " + testInput);
        
        String result = FilenameUtils.convertToSafeFilename(testInput);
        System.out.println("处理结果: " + result);
        
        // 分析每个字符的处理情况
        char[] chars = testInput.toCharArray();
        System.out.println("\n逐字符分析:");
        for (int i = 0; i < chars.length; i++) {
            char ch = chars[i];
            String charStr = String.valueOf(ch);
            boolean isReplaced = !result.contains(charStr) && 
                               specialCharsPattern.matcher(charStr).matches();
            System.out.printf("位置%d: '%c' (U+%04X) -> %s%n", 
                            i, ch, (int)ch, 
                            isReplaced ? "替换为空格" : "保留");
        }
        
        // 检查特定字符
        System.out.println("\n特定字符检查:");
        System.out.println("'\"' (双引号): " + (result.contains("\"") ? "保留" : "替换"));
        System.out.println("''' (单引号): " + (result.contains("'") ? "保留" : "替换"));
        System.out.println("',' (逗号): " + (result.contains(",") ? "保留" : "替换"));
        System.out.println("'、' (顿号): " + (result.contains("、") ? "保留" : "替换"));
        System.out.println("';' (分号): " + (result.contains(";") ? "保留" : "替换"));
        System.out.println("':' (冒号): " + (result.contains(":") ? "保留" : "替换"));
        
        // 验证空格合并
        System.out.println("\n空格合并验证:");
        String multipleSpacesTest = "测试！！！多个！！！感叹号";
        String multipleResult = FilenameUtils.convertToSafeFilename(multipleSpacesTest);
        System.out.println("输入: " + multipleSpacesTest);
        System.out.println("输出: " + multipleResult);
        System.out.println("是否包含连续空格: " + multipleResult.contains("  "));
        
        // 验证最终结果符合预期
        assertEquals("测试 双引号 单引号 .pdf", result);
    }
}