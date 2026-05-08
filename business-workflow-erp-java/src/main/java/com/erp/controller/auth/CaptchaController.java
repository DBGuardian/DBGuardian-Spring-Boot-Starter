package com.erp.controller.auth;

import com.erp.common.constant.RedisConstant;
import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;

/**
 * 验证码控制器
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Slf4j
@Api(tags = "验证码管理")
@RestController
@RequestMapping("/auth")
public class CaptchaController {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private Environment environment;

    private static final String CAPTCHA_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CAPTCHA_LENGTH = 4;
    private static final int CAPTCHA_WIDTH = 120;
    private static final int CAPTCHA_HEIGHT = 40;
    private static final int CAPTCHA_EXPIRE_MINUTES = 5;

    /**
     * 生成验证码
     */
    @ApiOperation("生成验证码")
    @GetMapping("/captcha")
    public Result<CaptchaResponse> generateCaptcha(HttpServletRequest request) {
        // 开发环境下直接返回固定验证码，不写入Redis，减少写压力
        if (isDevProfile()) {
            String captchaKey = "DEV-" + System.currentTimeMillis();
            String captchaValue = "9999";
            String imageBase64 = generateCaptchaImage(captchaValue);

            CaptchaResponse response = new CaptchaResponse();
            response.setCaptchaKey(captchaKey);
            response.setCaptchaImage(imageBase64);

            return Result.success(response);
        }

        // 生成验证码Key
        String captchaKey = "CAP-" + System.currentTimeMillis() + "-" + 
                           String.valueOf(new Random().nextInt(10000));

        // 生成验证码值
        String captchaValue = generateCaptchaValue();

        // 存储到Redis，5分钟过期
        String redisKey = RedisConstant.CAPTCHA_PREFIX + captchaKey;
        redisTemplate.opsForValue().set(redisKey, captchaValue, 
                                       CAPTCHA_EXPIRE_MINUTES, TimeUnit.MINUTES);

        // 生成验证码图片
        String imageBase64 = generateCaptchaImage(captchaValue);

        CaptchaResponse response = new CaptchaResponse();
        response.setCaptchaKey(captchaKey);
        response.setCaptchaImage(imageBase64);

        return Result.success(response);
    }

    /**
     * 生成验证码值
     */
    private String generateCaptchaValue() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CAPTCHA_LENGTH; i++) {
            int index = random.nextInt(CAPTCHA_CHARS.length());
            sb.append(CAPTCHA_CHARS.charAt(index));
        }
        return sb.toString();
    }

    /**
     * 生成验证码图片（Base64）
     */
    private String generateCaptchaImage(String text) {
        BufferedImage image = new BufferedImage(CAPTCHA_WIDTH, CAPTCHA_HEIGHT, 
                                               BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // 设置抗锯齿
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                          RenderingHints.VALUE_ANTIALIAS_ON);

        // 填充背景
        g.setColor(new Color(238, 242, 255));
        g.fillRect(0, 0, CAPTCHA_WIDTH, CAPTCHA_HEIGHT);

        // 绘制验证码文字
        g.setColor(new Color(29, 78, 216));
        g.setFont(new Font("Verdana", Font.BOLD, 22));
        FontMetrics fm = g.getFontMetrics();
        int x = (CAPTCHA_WIDTH - fm.stringWidth(text)) / 2;
        int y = (CAPTCHA_HEIGHT + fm.getAscent() - fm.getDescent()) / 2;
        g.drawString(text, x, y);

        // 添加干扰线
        Random random = new Random();
        g.setColor(new Color(200, 200, 200));
        for (int i = 0; i < 3; i++) {
            int x1 = random.nextInt(CAPTCHA_WIDTH);
            int y1 = random.nextInt(CAPTCHA_HEIGHT);
            int x2 = random.nextInt(CAPTCHA_WIDTH);
            int y2 = random.nextInt(CAPTCHA_HEIGHT);
            g.drawLine(x1, y1, x2, y2);
        }

        g.dispose();

        // 转换为Base64
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            byte[] imageBytes = baos.toByteArray();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
        } catch (IOException e) {
            log.error("生成验证码图片失败", e);
            return "";
        }
    }

    /**
     * 验证码响应DTO
     */
    public static class CaptchaResponse {
        private String captchaKey;
        private String captchaImage;

        public String getCaptchaKey() {
            return captchaKey;
        }

        public void setCaptchaKey(String captchaKey) {
            this.captchaKey = captchaKey;
        }

        public String getCaptchaImage() {
            return captchaImage;
        }

        public void setCaptchaImage(String captchaImage) {
            this.captchaImage = captchaImage;
        }
    }

    /**
     * 是否为开发环境
     */
    private boolean isDevProfile() {

        return environment != null && environment.acceptsProfiles("dev");
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(ip)) {
            // X-Forwarded-For 可能包含多个IP，取第一个
            int index = ip.indexOf(',');
            return index > 0 ? ip.substring(0, index).trim() : ip.trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(ip)) {
            return ip.trim();
        }
        return request.getRemoteAddr();
    }
}



