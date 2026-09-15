package com.dms.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dms.auth.JwtService;
import com.dms.auth.LoginUser;
import com.dms.auth.Role;
import com.dms.auth.UserContext;
import com.dms.auth.entity.SysUser;
import com.dms.auth.mapper.SysUserMapper;
import com.dms.common.BizException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private static final int MAX_FAILURES = 5;
    private static final long LOCK_MILLIS = 15 * 60 * 1000L;
    private static final int MAX_ENTRIES = 10_000;

    private final SysUserMapper userMapper;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final ConcurrentHashMap<String, FailInfo> failures = new ConcurrentHashMap<>();
    private volatile long lockMillis = LOCK_MILLIS;

    private static class FailInfo {
        int count;
        long lockUntil;
        long lastFailure;
    }

    public AuthService(SysUserMapper userMapper, JwtService jwtService) {
        this.userMapper = userMapper;
        this.jwtService = jwtService;
    }

    public String hashPassword(String raw) {
        return encoder.encode(raw);
    }

    public Map<String, Object> login(String username, String password) {
        if (username == null
                || username.trim().isEmpty()
                || password == null
                || password.isEmpty()) {
            throw new BizException(400, "用户名和密码必填");
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
            recordFailure(username);
            throw new BizException(401, "用户名或密码错误");
        }
        if (Role.of(u.getRole()) == null) {
            recordFailure(username);
            throw new BizException(401, "用户名或密码错误");
        }
        failures.remove(username);
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

    private void recordFailure(String username) {
        FailInfo fi = failures.computeIfAbsent(username, k -> new FailInfo());
        synchronized (fi) {
            fi.count++;
            fi.lastFailure = System.currentTimeMillis();
            if (fi.count >= MAX_FAILURES) {
                fi.lockUntil = fi.lastFailure + lockMillis;
            }
        }
        evictIfNeeded();
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
                if (e.getValue().lastFailure < oldestTs) {
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
