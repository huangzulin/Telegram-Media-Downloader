package fun.zulin.tmd.utils;

import lombok.extern.slf4j.Slf4j;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * 文件名处理工具类
 * 用于处理包含特殊字符的文件名
 */
@Slf4j
public class FilenameUtils {
    
    // 需要替换为空格的特殊字符模式
    private static final Pattern SPECIAL_CHARS_PATTERN = Pattern.compile("[\\[\\](){}<>:\"'/\\\\|?*\\x00-\\x1F【】（）。，、；：？！‘’“”…]+");
    
    /**
     * 将原始描述转换为安全的文件名
     * @param description 原始文件描述
     * @return 处理后的文件名
     */
    public static String convertToSafeFilename(String description) {
        if (description == null || description.trim().isEmpty()) {
            return "unnamed_file_" + System.currentTimeMillis();
        }
        
        String processed = description.trim();
        
        // 1. 将所有特殊字符替换为空格
        processed = SPECIAL_CHARS_PATTERN.matcher(processed).replaceAll(" ");
        
        // 2. 处理多余空格（多个空格合并为一个）
        processed = processed.replaceAll("\\s+", " ").trim();
        
        // 3. 移除开头和结尾的空格
        processed = processed.trim();
        
        // 5. 限制长度（避免文件名过长）
        if (processed.length() > 200) {
            int lastDotIndex = processed.lastIndexOf('.');
            if (lastDotIndex > 0) {
                String extension = processed.substring(lastDotIndex);
                processed = processed.substring(0, 200 - extension.length()) + extension;
            } else {
                processed = processed.substring(0, 200);
            }
        }
        
        // 6. 确保不为空
        if (processed.isEmpty()) {
            processed = "unnamed_file_" + System.currentTimeMillis();
        }
        
        log.debug("文件名转换: '{}' -> '{}'", description, processed);
        return processed;
    }
    
    /**
     * 对文件名进行URL编码
     * @param filename 文件名
     * @return 编码后的文件名
     */
    public static String encodeFilename(String filename) {
        if (filename == null) return null;
        try {
            return URLEncoder.encode(filename, StandardCharsets.UTF_8.toString())
                    .replace("+", "%20"); // 将+替换为%20以保持空格的一致性
        } catch (Exception e) {
            log.error("文件名编码失败: {}", filename, e);
            return filename;
        }
    }
    
    /**
     * 对文件名进行URL解码
     * @param encodedFilename 编码后的文件名
     * @return 解码后的文件名
     */
    public static String decodeFilename(String encodedFilename) {
        if (encodedFilename == null) return null;
        try {
            return URLDecoder.decode(encodedFilename, StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            log.error("文件名解码失败: {}", encodedFilename, e);
            return encodedFilename;
        }
    }
    
    /**
     * 标准化文件名用于比较和匹配
     * @param filename 文件名
     * @return 标准化后的文件名
     */
    public static String normalizeFilename(String filename) {
        if (filename == null) return "";
        
        return filename
                .toLowerCase()
                .replaceAll("[\\[\\](){}]", " ")  // 替换括号为空格
                .replaceAll("[,，;；:：]", " ")    // 替换标点为空格
                .replaceAll("\\s+", " ")          // 统一空格
                .trim();
    }
    
    /**
     * 检查文件名是否包含特殊字符需要编码
     * @param filename 文件名
     * @return 是否需要编码
     */
    public static boolean needsEncoding(String filename) {
        if (filename == null) return false;
        return !filename.equals(encodeFilename(decodeFilename(filename)));
    }
}