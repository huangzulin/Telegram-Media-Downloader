package fun.zulin.tmd.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 二维码生成服务
 */
@Slf4j
@Service
public class QRCodeService {

    private static final int DEFAULT_WIDTH = 250;
    private static final int DEFAULT_HEIGHT = 250;
    private static final String DEFAULT_CHARSET = "UTF-8";

    /**
     * 生成二维码图片并返回Base64编码
     *
     * @param content 二维码内容
     * @return Base64编码的PNG图片数据
     */
    public String generateQRCode(String content) {
        return generateQRCode(content, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    /**
     * 生成指定尺寸的二维码图片并返回Base64编码
     *
     * @param content 二维码内容
     * @param width   图片宽度
     * @param height  图片高度
     * @return Base64编码的PNG图片数据
     */
    public String generateQRCode(String content, int width, int height) {
        try {
            // 设置二维码参数
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, DEFAULT_CHARSET);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 1);

            // 创建二维码写入器
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height, hints);

            // 创建BufferedImage
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();

            // 设置背景色为白色
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, width, height);

            // 设置前景色为黑色
            graphics.setColor(Color.BLACK);

            // 绘制二维码点阵
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    if (bitMatrix.get(x, y)) {
                        graphics.fillRect(x, y, 1, 1);
                    }
                }
            }

            graphics.dispose();

            // 转换为Base64
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", outputStream);
            
            String base64Image = Base64.getEncoder().encodeToString(outputStream.toByteArray());
            log.debug("二维码生成成功，内容长度: {}, 图片尺寸: {}x{}", 
                     content.length(), width, height);
            
            return base64Image;

        } catch (WriterException | java.io.IOException e) {
            log.error("生成二维码失败: content={}, error={}", content, e.getMessage(), e);
            throw new RuntimeException("二维码生成失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成带logo的二维码（可选功能）
     *
     * @param content 二维码内容
     * @param logoPath logo图片路径
     * @return Base64编码的PNG图片数据
     */
    public String generateQRCodeWithLogo(String content, String logoPath) {
        // TODO: 实现带logo的二维码生成功能
        return generateQRCode(content);
    }

    /**
     * 验证二维码内容是否有效
     *
     * @param content 二维码内容
     * @return 是否有效
     */
    public boolean isValidContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }
        
        // 检查内容长度（二维码有容量限制）
        // Version 1 (21x21) 最多可存储约 25 个字符
        // 实际应用中可以根据需要调整限制
        return content.length() <= 1000;
    }
}