package com.dms.auth;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dms.auth.entity.SysUser;
import com.dms.auth.mapper.SysUserMapper;
import com.dms.auth.service.AuthService;
import java.security.SecureRandom;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** 首次启动（sys_user 为空）时创建引导 admin 账号。 */
@Slf4j
@Component
public class AdminBootstrap implements ApplicationRunner {
    private static final String ALPHANUM =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";

    private final SysUserMapper userMapper;
    private final AuthService authService;
    private final String configuredPassword;

    public AdminBootstrap(
            SysUserMapper userMapper,
            AuthService authService,
            @Value("${dms.auth.bootstrap-admin-password:}") String configuredPassword) {
        this.userMapper = userMapper;
        this.authService = authService;
        this.configuredPassword = configuredPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        Long count = userMapper.selectCount(new QueryWrapper<SysUser>());
        if (count != null && count > 0) {
            return;
        }
        String password = configuredPassword;
        if (password == null || password.trim().isEmpty()) {
            password = randomPassword();
            log.warn("首次启动已创建 admin，初始密码: {}，请立即修改", password);
        } else {
            log.info("首次启动已创建 admin，请尽快修改初始密码");
        }
        SysUser u = new SysUser();
        u.setUsername("admin");
        u.setPasswordHash(authService.hashPassword(password));
        u.setRealName("系统管理员");
        u.setRole(Role.ADMIN.name());
        u.setEnabled(true);
        userMapper.insert(u);
    }

    private String randomPassword() {
        SecureRandom r = new SecureRandom();
        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            sb.append(ALPHANUM.charAt(r.nextInt(ALPHANUM.length())));
        }
        return sb.toString();
    }
}
