package cn.gaifan.douyinOperations.module.auth.service.impl;

import cn.gaifan.douyinOperations.module.auth.service.CaptchaService;
import cn.gaifan.douyinOperations.module.auth.vo.CaptchaVO;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 图片验证码：内存缓存，5 分钟有效；按 IP 记录密码失败，失败后 15 分钟内需验证码
 */
@Service
public class CaptchaServiceImpl implements CaptchaService {

    private static final int CAPTCHA_TTL_MINUTES = 5;
    private static final int REQUIRE_CAPTCHA_MINUTES = 15;
    private static final long REQUIRE_CAPTCHA_MS = REQUIRE_CAPTCHA_MINUTES * 60 * 1000L;
    private static final int IMG_WIDTH = 120;
    private static final int IMG_HEIGHT = 40;
    private static final int CODE_LENGTH = 4;
    private static final String CODE_CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";

    /** captchaId -> code，校验后移除 */
    private final Map<String, CaptchaEntry> codeCache = new ConcurrentHashMap<>();
    /** clientIp -> lastFailureTimeMillis */
    private final Map<String, Long> failureByIp = new ConcurrentHashMap<>();

    private static class CaptchaEntry {
        final String code;
        final long expireAt;

        CaptchaEntry(String code, long expireAt) {
            this.code = code;
            this.expireAt = expireAt;
        }
    }

    @Override
    public CaptchaVO generate() {
        String code = randomCode();
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        long expireAt = System.currentTimeMillis() + CAPTCHA_TTL_MINUTES * 60 * 1000L;
        codeCache.put(captchaId, new CaptchaEntry(code, expireAt));
        String imageBase64 = drawImage(code);
        return new CaptchaVO(captchaId, imageBase64);
    }

    @Override
    public boolean validate(String captchaId, String code) {
        if (captchaId == null || code == null) return false;
        CaptchaEntry entry = codeCache.remove(captchaId);
        if (entry == null) return false;
        if (System.currentTimeMillis() > entry.expireAt) return false;
        return entry.code.equalsIgnoreCase(code.trim());
    }

    @Override
    public void recordLoginFailure(String clientIp) {
        if (clientIp == null || clientIp.isEmpty()) return;
        failureByIp.put(clientIp, System.currentTimeMillis());
    }

    @Override
    public boolean requireCaptcha(String clientIp) {
        // 开发环境禁用验证码要求
        return false;
    }

    @Override
    public int getRequireCaptchaMinutes() {
        return REQUIRE_CAPTCHA_MINUTES;
    }

    private String randomCode() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_CHARS.charAt(r.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }

    private String drawImage(String code) {
        BufferedImage image = new BufferedImage(IMG_WIDTH, IMG_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(240, 240, 240));
        g.fillRect(0, 0, IMG_WIDTH, IMG_HEIGHT);
        g.setColor(new Color(60, 60, 60));
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        int x = 12;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < code.length(); i++) {
            g.setColor(new Color(r.nextInt(80) + 20, r.nextInt(80) + 20, r.nextInt(80) + 20));
            g.drawString(String.valueOf(code.charAt(i)), x, 28);
            x += 26;
        }
        g.dispose();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("生成验证码图片失败", e);
        }
    }
}
