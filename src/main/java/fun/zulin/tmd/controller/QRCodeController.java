package fun.zulin.tmd.controller;

import fun.zulin.tmd.common.exception.ApiResponse;
import fun.zulin.tmd.service.QRCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 二维码API控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/qrcode")
@RequiredArgsConstructor
public class QRCodeController {

    private final QRCodeService qrCodeService;

    @PostMapping("/generate")
    public ApiResponse<Map<String, String>> generateQRCode(
            @RequestBody Map<String, String> request) {
        
        try {
            String content = request.get("content");
            if (content == null || content.trim().isEmpty()) {
                return ApiResponse.error(400, "二维码内容不能为空");
            }

            if (!qrCodeService.isValidContent(content)) {
                return ApiResponse.error(400, "二维码内容过长或格式不正确");
            }

            String base64Image = qrCodeService.generateQRCode(content);
            
            Map<String, String> result = new HashMap<>();
            result.put("image", base64Image);
            result.put("content", content);
            
            log.info("二维码生成成功: contentLength={}", content.length());
            return ApiResponse.success(result);
            
        } catch (Exception e) {
            log.error("生成二维码失败: ", e);
            return ApiResponse.error(500, "生成二维码失败: " + e.getMessage());
        }
    }

    @GetMapping("/generate")
    public ApiResponse<Map<String, String>> generateQRCodeGet(
            @RequestParam String content,
            @RequestParam(defaultValue = "250") Integer width,
            @RequestParam(defaultValue = "250") Integer height) {
        
        try {
            if (content == null || content.trim().isEmpty()) {
                return ApiResponse.error(400, "二维码内容不能为空");
            }

            if (!qrCodeService.isValidContent(content)) {
                return ApiResponse.error(400, "二维码内容过长或格式不正确");
            }

            // 限制尺寸范围
            width = Math.max(100, Math.min(500, width));
            height = Math.max(100, Math.min(500, height));

            String base64Image = qrCodeService.generateQRCode(content, width, height);
            
            Map<String, String> result = new HashMap<>();
            result.put("image", base64Image);
            result.put("content", content);
            result.put("width", width.toString());
            result.put("height", height.toString());
            
            log.info("二维码生成成功: contentLength={}, size={}x{}", 
                     content.length(), width, height);
            return ApiResponse.success(result);
            
        } catch (Exception e) {
            log.error("生成二维码失败: ", e);
            return ApiResponse.error(500, "生成二维码失败: " + e.getMessage());
        }
    }

    @PostMapping("/validate")
    public ApiResponse<Map<String, Object>> validateContent(
            @RequestBody Map<String, String> request) {
        
        try {
            String content = request.get("content");
            boolean isValid = qrCodeService.isValidContent(content);
            
            Map<String, Object> result = new HashMap<>();
            result.put("valid", isValid);
            result.put("contentLength", content != null ? content.length() : 0);
            result.put("maxLength", 1000);
            
            if (!isValid && content != null) {
                result.put("message", "内容长度超过限制或格式不正确");
            }
            
            return ApiResponse.success(result);
            
        } catch (Exception e) {
            log.error("验证二维码内容失败: ", e);
            return ApiResponse.error(500, "验证失败: " + e.getMessage());
        }
    }
}