package com.dms.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dms.auth.CaptchaService;
import com.dms.auth.JwtService;
import com.dms.auth.LoginRateLimiter;
import com.dms.auth.LoginUser;
import com.dms.auth.Role;
import com.dms.auth.UserContext;
import com.dms.auth.entity.SysUser;
import com.dms.auth.mapper.SysUserMapper;
import com.dms.common.BizException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private static final int MAX_FAILURES = 5;
    private static final long LOCK_MILLIS = 15 * 60 * 1000L;
    private static final int MAX_ENTRIES = 10_000;

    public static final int CODE_CAPTCHA_REQUIRED = 4001;

    private final SysUserMapper userMapper;
    private final JwtService jwtService;
    private final LoginRateLimiter rateLimiter;
    private final CaptchaService captchaService;
    private final String captchaMode;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final ConcurrentHashMap<String, FailInfo> failures = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, IpFailInfo> ipFailures = new ConcurrentHashMap<>();
    private volatile long lockMillis = LOCK_MILLIS;

    private static class IpFailInfo {
        java.util.concurrent.atomic.AtomicInteger count =
                new java.util.concurrent.atomic.AtomicInteger();
        volatile long lastFailure;
    }

    private static class FailInfo {
        int count;
        long lockUntil;
        long lastFailure;
    }

    public AuthService(
            SysUserMapper userMapper,
            JwtService jwtService,
            LoginRateLimiter rateLimiter,
            CaptchaService captchaService,
            @Value("${dms.auth.captcha-mode:ADAPTIVE}") String captchaMode) {
        this.userMapper = userMapper;
        this.jwtService = jwtService;
        this.rateLimiter = rateLimiter;
        this.captchaService = captchaService;
        this.captchaMode = captchaMode == null ? "ADAPTIVE" : captchaMode.trim().toUpperCase();
    }

    public String hashPassword(String raw) {
        return encoder.encode(raw);
    }

    /** 该用户名当前是否要求验证码（供前端预检）。 */
    public boolean captchaRequired(String username, HttpServletRequest request) {
        if ("OFF".equals(captchaMode)) {
            return false;
        }
        if ("ALWAYS".equals(captchaMode)) {
            return true;
        }
        long now = System.currentTimeMillis();
        if (username != null && !username.trim().isEmpty()) {
            FailInfo fi = failures.get(username);
            if (fi != null && fi.count >= 3) {
                return true;
            }
        }
        if (request != null) {
            IpFailInfo ip = ipFailures.get(rateLimiter.clientIp(request));
            if (ip != null
                    && ip.count.get() >= 3
                    && now - ip.lastFailure <= lockMillis) {
                return true;
            }
        }
        return false;
    }

    public Map<String, Object> login(
            String username,
            String password,
            String captchaId,
            String captchaCode,
            HttpServletRequest request) {
        String ip = rateLimiter.clientIp(request);
        rateLimiter.check(ip);
        if (username == null
                || username.trim().isEmpty()
                || password == null
                || password.isEmpty()) {
            throw new BizException(400, "用户名和密码必填");
        }
        if (captchaRequired(username, request)
                && !captchaService.verify(captchaId, captchaCode)) {
            throw new BizException(CODE_CAPTCHA_REQUIRED, "请输入正确的验证码");
        }
        FailInfo fi = failures.get(username);
        if (fi != null) {
            long now = System.currentTimeMillis();
            if (fi.lockUntil > now) {
                throw new BizException(401, "登录失败次数过多，请15分钟后再试");
            }
            if (fi.lockUntil > 0) {
                synchronized (fi) {
                    if (fi.lockUntil > 0 && fi.lockUntil <= now) {
                        fi.count = 0;
                        fi.lockUntil = 0;
                    }
                }
            }
        }
        SysUser u =
                userMapper.selectOne(
                        new QueryWrapper<SysUser>().eq("username", username).last("LIMIT 1"));
        if (u == null
                || !Boolean.TRUE.equals(u.getEnabled())
                || u.getPasswordHash() == null
                || password == null
                || !encoder.matches(password, u.getPasswordHash())) {
            recordFailure(username, ip);
            throw new BizException(401, "用户名或密码错误");
        }
        if (Role.of(u.getRole()) == null) {
            recordFailure(username, ip);
            throw new BizException(401, "用户名或密码错误");
        }
        failures.remove(username);
        ipFailures.remove(ip);
        LoginUser login = toLoginUser(u);
        Map<String, Object> m = new HashMap<>();
        m.put("token", jwtService.issue(login));
        Map<String, Object> user = new HashMap<>();
        user.put("id", u.getId());
        user.put("username", u.getUsername());
        user.put("realName", u.getRealName());
        user.put("role", u.getRole());
        user.put("dealerCode", u.getDealerCode());
        m.put("user", user);
        return m;
    }

    public SysUser me() {
        LoginUser login = UserContext.get();
        if (login == null) {
            throw new BizException(401, "未登录或登录已过期");
        }
        return userMapper.selectById(login.getId());
    }

    public void changePassword(String oldPwd, String newPwd) {
        if (newPwd == null || newPwd.trim().length() < 6) {
            throw new BizException("新密码至少 6 位");
        }
        SysUser u = me();
        if (u == null
                || u.getPasswordHash() == null
                || oldPwd == null
                || !encoder.matches(oldPwd, u.getPasswordHash())) {
            throw new BizException("原密码错误");
        }
        u.setPasswordHash(encoder.encode(newPwd));
        userMapper.updateById(u);
    }

    private void recordFailure(String username, String ip) {
        FailInfo fi = failures.computeIfAbsent(username, k -> new FailInfo());
        synchronized (fi) {
            fi.count++;
            fi.lastFailure = System.currentTimeMillis();
            if (fi.count >= MAX_FAILURES) {
                fi.lockUntil = fi.lastFailure + lockMillis;
            }
        }
        if (ip != null) {
            IpFailInfo ipfi = ipFailures.computeIfAbsent(ip, k -> new IpFailInfo());
            ipfi.count.incrementAndGet();
            ipfi.lastFailure = fi.lastFailure;
        }
        evictIfNeeded();
        evictIpIfNeeded();
    }

    private void evictIfNeeded() {
        if (failures.size() <= MAX_ENTRIES) {
            return;
        }
        long now = System.currentTimeMillis();
        failures
                .entrySet()
                .removeIf(
                        e -> e.getValue().lockUntil <= now
                                && now - e.getValue().lastFailure > lockMillis);
        while (failures.size() > MAX_ENTRIES) {
            String oldest = null;
            long oldestTs = Long.MAX_VALUE;
            for (Map.Entry<String, FailInfo> e : failures.entrySet()) {
                if (e.getValue().lockUntil <= now && e.getValue().lastFailure < oldestTs) {
                    oldestTs = e.getValue().lastFailure;
                    oldest = e.getKey();
                }
            }
            if (oldest == null) {
                break;
            }
            failures.remove(oldest);
        }
    }

    private void evictIpIfNeeded() {
        if (ipFailures.size() <= MAX_ENTRIES) {
            return;
        }
        long now = System.currentTimeMillis();
        ipFailures.entrySet().removeIf(e -> now - e.getValue().lastFailure > lockMillis);
        while (ipFailures.size() > MAX_ENTRIES) {
            String oldest = null;
            long oldestTs = Long.MAX_VALUE;
            for (Map.Entry<String, IpFailInfo> e : ipFailures.entrySet()) {
                if (e.getValue().lastFailure < oldestTs) {
                    oldestTs = e.getValue().lastFailure;
                    oldest = e.getKey();
                }
            }
            if (oldest == null) {
                break;
            }
            ipFailures.remove(oldest);
        }
    }

    /** 测试用：清空锁定状态。 */
    public void clearLock(String username) {
        failures.remove(username);
    }

    /** 测试用：调整锁定时长。 */
    public void setLockMillisForTest(long ms) {
        this.lockMillis = ms;
    }

    private LoginUser toLoginUser(SysUser u) {
        return new LoginUser(u.getId(), u.getUsername(), Role.of(u.getRole()), u.getDealerCode());
    }
}
