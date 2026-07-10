package com.audit.common.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Random;

/**
 * 图形验证码工具类
 * <p>生成4位数字+字母混合验证码，含干扰线+噪点，输出Base64</p>
 */
public final class CaptchaUtil {

    /** 字符池：数字+大小写字母，去除易混淆字符 0OIl1 */
    private static final String CHAR_POOL = "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz";
    private static final int CODE_LEN = 4;
    private static final int WIDTH = 120;
    private static final int HEIGHT = 44;
    private static final Random RANDOM = new Random();

    private CaptchaUtil() {}

    /** 生成4位随机验证码文本 */
    public static String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LEN);
        for (int i = 0; i < CODE_LEN; i++) {
            sb.append(CHAR_POOL.charAt(RANDOM.nextInt(CHAR_POOL.length())));
        }
        return sb.toString();
    }

    /** 根据验证码文本生成Base64 PNG图片 */
    public static String generateImage(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // 1. 白色背景
        g.setColor(new Color(245, 248, 255));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // 2. 干扰线（5条随机曲线）
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (int i = 0; i < 5; i++) {
            g.setColor(randomColor(120, 200));
            int x1 = RANDOM.nextInt(WIDTH / 3);
            int y1 = RANDOM.nextInt(HEIGHT);
            int x2 = WIDTH - RANDOM.nextInt(WIDTH / 3);
            int y2 = RANDOM.nextInt(HEIGHT);
            g.drawLine(x1, y1, x2, y2);
        }

        // 3. 噪点（60个随机颜色点）
        for (int i = 0; i < 60; i++) {
            g.setColor(randomColor(100, 200));
            g.fillRect(RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT), 1, 1);
        }

        // 4. 绘制验证码文字（每个字符不同颜色+角度）
        int fontSize = 26;
        Font[] fonts = {
                new Font("Arial", Font.BOLD, fontSize),
                new Font("Helvetica", Font.ITALIC, fontSize),
                new Font("Courier New", Font.BOLD, fontSize)
        };
        for (int i = 0; i < code.length(); i++) {
            g.setColor(randomColor(20, 120));
            g.setFont(fonts[RANDOM.nextInt(fonts.length)]);
            // 随机旋转角度 (-25° ~ 25°)
            double angle = Math.toRadians((RANDOM.nextDouble() - 0.5) * 50);
            g.rotate(angle, 20 + i * 24, 28 + RANDOM.nextInt(6));
            g.drawString(String.valueOf(code.charAt(i)), 16 + i * 24, 30);
            g.rotate(-angle, 20 + i * 24, 28 + RANDOM.nextInt(6));
        }

        g.dispose();

        // 5. 输出Base64
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("验证码图片生成失败", e);
        }
    }

    private static Color randomColor(int min, int max) {
        int r = min + RANDOM.nextInt(max - min);
        int g = min + RANDOM.nextInt(max - min);
        int b = min + RANDOM.nextInt(max - min);
        return new Color(r, g, b);
    }
}
