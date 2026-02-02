package fun.zulin.tmd.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DebugSpecialCharsTest {

    @Test
    void debugPureSpecialChars() {
        String test6 = "【】（）！@#$%^&*()_+-=[]{}|;':\",./<>?~`";
        String result6 = FilenameUtils.convertToSafeFilename(test6);
        
        System.out.println("原始输入: " + test6);
        System.out.println("处理结果: '" + result6 + "'");
        System.out.println("结果长度: " + result6.length());
        System.out.println("是否为空: " + result6.isEmpty());
        System.out.println("是否以unnamed_file_开头: " + result6.startsWith("unnamed_file_"));
        
        // 检查每个字符
        for (int i = 0; i < test6.length(); i++) {
            char ch = test6.charAt(i);
            boolean isInResult = result6.indexOf(ch) >= 0;
            System.out.printf("字符 '%c' (U+%04X): %s%n", ch, (int)ch, 
                            isInResult ? "保留在结果中" : "被移除或替换");
        }
        
        // 最终断言
        assertTrue(result6.isEmpty() || result6.startsWith("unnamed_file_"), 
                  "纯特殊字符应该变为空字符串或默认文件名");
    }
}