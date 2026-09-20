package com.dms.auth;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Component;

/** 图形验证码：内存存储，一次性，5 分钟过期。 */
@Component
public class CaptchaService {
    private static final String CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final long TTL_MILLIS = 5 * 60 * 1000L;
    private static final int MAX_ENTRIES = 10_000;
    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;

    private final SecureRandom random = new SecureRandom();
    private final ConcurrentHashMap<String, Entry> store = new ConcurrentHashMap<>();

    private static class Entry {
        String code;
        long expireAt;
    }

    public Map<String, String> create() {
        String code = randomCode(4);
        String id = UUID.randomUUID().toString();
        Entry e = new Entry();
        e.code = code;
        e.expireAt = System.currentTimeMillis() + TTL_MILLIS;
        store.put(id, e);
        evictIfNeeded();
        Map<String, String> m = new HashMap<>();
        m.put("captchaId", id);
        m.put("image", "data:image/png;base64," + render(code));
        return m;
    }

    public boolean verify(String id, String code) {
        if (id == null || code == null) {
            return false;
        }
        Entry e = store.remove(id);
        return e != null
                && e.expireAt >= System.currentTimeMillis()
                && e.code.equalsIgnoreCase(code.trim());
    }

    /** 测试用：读取验证码明文（不移除）。 */
    String peek(String id) {
        Entry e = store.get(id);
        return e == null ? null : e.code;
    }

    private void evictIfNeeded() {
        if (store.size() <= MAX_ENTRIES) {
            return;
        }
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(e -> e.getValue().expireAt < now);
        while (store.size() > MAX_ENTRIES) {
            String oldest = null;
            long oldestTs = Long.MAX_VALUE;
            for (Map.Entry<String, Entry> e : store.entrySet()) {
                if (e.getValue().expireAt < oldestTs) {
                    oldestTs = e.getValue().expireAt;
                    oldest = e.getKey();
                }
            }
            if (oldest == null) {
                break;
            }
            store.remove(oldest);
        }
    }

    private String randomCode(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    private String render(String code) {
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setColor(new Color(245, 247, 250));
            g.fillRect(0, 0, WIDTH, HEIGHT);
            for (int i = 0; i < 12; i++) {
                g.setColor(
                        new Color(
                                150 + random.nextInt(100),
                                150 + random.nextInt(100),
                                150 + random.nextInt(100)));
                g.drawLine(
                        random.nextInt(WIDTH),
                        random.nextInt(HEIGHT),
                        random.nextInt(WIDTH),
                        random.nextInt(HEIGHT));
            }
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
            for (int i = 0; i < code.length(); i++) {
                g.setColor(
                        new Color(
                                20 + random.nextInt(110),
                                20 + random.nextInt(110),
                                20 + random.nextInt(110)));
                int x = 12 + i * 26;
                int y = 24 + random.nextInt(10);
                double angle = Math.toRadians(random.nextInt(30) - 15);
                g.rotate(angle, x, y);
                g.drawString(String.valueOf(code.charAt(i)), x, y);
                g.rotate(-angle, x, y);
            }
        } finally {
            g.dispose();
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
