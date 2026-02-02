package fun.zulin.tmd.utils;

public class TestFilenameUtils {
    public static void main(String[] args) {
        String original = "极品大长腿丝袜御姐【小林的涩涩日常】SVIP1月13日新作，【被下属反攻的女上司】.mp4";
        String result = FilenameUtils.convertToSafeFilename(original);
        System.out.println("原始: " + original);
        System.out.println("结果: " + result);
    }
}