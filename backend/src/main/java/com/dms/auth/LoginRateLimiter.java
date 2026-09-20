package com.dms.auth;

import com.dms.common.BizException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 每客户端 IP 固定 60 秒窗口限流。 */
@Component
public class LoginRateLimiter {
    private static final long WINDOW_MILLIS = 60 * 1000L;
    private static final int MAX_ENTRIES = 10_000;

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final int maxPerMinute;
    private final boolean trustProxy;

    private static class Window {
        long start;
        int count;
    }

    public LoginRateLimiter(
            @Value("${dms.auth.ip-max-per-minute:30}") int maxPerMinute,
            @Value("${dms.auth.trust-proxy:false}") boolean trustProxy) {
        this.maxPerMinute = maxPerMinute;
        this.trustProxy = trustProxy;
    }

    public String clientIp(HttpServletRequest request) {
        if (trustProxy) {
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.trim().isEmpty()) {
                return xff.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    public void check(String ip) {
        if (maxPerMinute <= 0) {
            return;
        }
        long now = System.currentTimeMillis();
        Window w = windows.computeIfAbsent(ip, k -> new Window());
        synchronized (w) {
            if (now - w.start >= WINDOW_MILLIS) {
                w.start = now;
                w.count = 0;
            }
            w.count++;
            if (w.count > maxPerMinute) {
                throw new BizException(429, "请求过于频繁，请稍后再试");
            }
        }
        evictIfNeeded(now);
    }

    private void evictIfNeeded(long now) {
        if (windows.size() <= MAX_ENTRIES) {
            return;
        }
        windows.entrySet().removeIf(e -> now - e.getValue().start >= WINDOW_MILLIS);
        while (windows.size() > MAX_ENTRIES) {
            String oldest = null;
            long oldestTs = Long.MAX_VALUE;
            for (Map.Entry<String, Window> e : windows.entrySet()) {
                if (e.getValue().start < oldestTs) {
                    oldestTs = e.getValue().start;
                    oldest = e.getKey();
                }
            }
            if (oldest == null) {
                break;
            }
            windows.remove(oldest);
        }
    }
}
