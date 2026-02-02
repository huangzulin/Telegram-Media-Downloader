package fun.zulin.tmd.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DebugFilenameTest {

    @Test
    void debugConvertToSafeFilename() {
        // 测试中文特殊字符转换
        String original = "极品大长腿丝袜御姐【小林的涩涩日常】SVIP1月13日新作，【被下属反攻的女上司】.mp4";
        String result = FilenameUtils.convertToSafeFilename(original);
        
        System.out.println("原始输入: " + original);
        System.out.println("处理结果: " + result);
        System.out.println("是否包含关键内容:");
        System.out.println("  包含'极品大长腿丝袜御姐': " + result.contains("极品大长腿丝袜御姐"));
        System.out.println("  包含'[小林的涩涩日常]': " + result.contains("[小林的涩涩日常]"));
        System.out.println("  包含'SVIP1月13日新作': " + result.contains("SVIP1月13日新作"));
        System.out.println("  包含',': " + result.contains(","));
        System.out.println("  包含'[被下属反攻的女上司]': " + result.contains("[被下属反攻的女上司]"));
        System.out.println("  以'.mp4'结尾: " + result.endsWith(".mp4"));
        
        // 详细分析每个断言
        boolean test1 = result.contains("极品大长腿丝袜御姐");
        boolean test2 = result.contains("[小林的涩涩日常]");
        boolean test3 = result.contains("SVIP1月13日新作");
        boolean test4 = result.contains(",");
        boolean test5 = result.contains("[被下属反攻的女上司]");
        boolean test6 = result.endsWith(".mp4");
        
        System.out.println("\n各断言结果:");
        System.out.println("test1 (包含'极品大长腿丝袜御姐'): " + test1);
        System.out.println("test2 (包含'[小林的涩涩日常]'): " + test2);
        System.out.println("test3 (包含'SVIP1月13日新作'): " + test3);
        System.out.println("test4 (包含','): " + test4);
        System.out.println("test5 (包含'[被下属反攻的女上司]'): " + test5);
        System.out.println("test6 (以'.mp4'结尾): " + test6);
        
        // 找出具体哪个断言失败
        if (!test1) System.out.println("FAIL: test1 失败");
        if (!test2) System.out.println("FAIL: test2 失败");
        if (!test3) System.out.println("FAIL: test3 失败");
        if (!test4) System.out.println("FAIL: test4 失败");
        if (!test5) System.out.println("FAIL: test5 失败");
        if (!test6) System.out.println("FAIL: test6 失败");
        
        // 这里应该是组合所有断言的结果
        assertTrue(test1 && test2 && test3 && test4 && test5 && test6, 
                  "文件名处理不符合预期，详情请查看上面的输出");
    }
}