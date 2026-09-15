package com.dms.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.SecureRandom;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JwtService {
    private final Key key;
    private final long expireMillis;

    public JwtService(
            @Value("${dms.auth.jwt-secret:}") String secret,
            @Value("${dms.auth.expire-hours:12}") long expireHours) {
        byte[] bytes;
        if (secret == null || secret.trim().isEmpty()) {
            bytes = new byte[64];
            new SecureRandom().nextBytes(bytes);
            log.warn("未配置 dms.auth.jwt-secret，已生成随机密钥，重启后旧 token 失效");
        } else {
            bytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        if (bytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(bytes, 0, padded, 0, bytes.length);
            bytes = padded;
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expireMillis = expireHours * 3600_000L;
    }

    public String issue(LoginUser user) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claim("uid", user.getId())
                .claim("username", user.getUsername())
                .claim("role", user.getRole().name())
                .claim("dealerCode", user.getDealerCode())
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + expireMillis))
                .signWith(key)
                .compact();
    }

    /** 解析失败/过期返回 null。 */
    public LoginUser parse(String token) {
        try {
            Claims c =
                    Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
            LoginUser u = new LoginUser();
            Object uid = c.get("uid");
            u.setId(uid instanceof Number ? ((Number) uid).longValue() : null);
            u.setUsername(c.get("username", String.class));
            u.setRole(Role.of(c.get("role", String.class)));
            u.setDealerCode(c.get("dealerCode", String.class));
            return u.getRole() == null ? null : u;
        } catch (Exception e) {
            return null;
        }
    }
}
