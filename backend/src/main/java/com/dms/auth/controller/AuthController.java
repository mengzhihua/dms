package com.dms.auth.controller;

import com.dms.auth.entity.SysUser;
import com.dms.auth.service.AuthService;
import com.dms.common.BizException;
import com.dms.common.R;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        return R.ok(service.login(body.get("username"), body.get("password")));
    }

    @GetMapping("/me")
    public R<SysUser> me() {
        SysUser u = service.me();
        return R.ok(u);
    }

    @PostMapping("/logout")
    public R<Void> logout() {
        return R.ok();
    }

    @PutMapping("/password")
    public R<Void> password(@RequestBody Map<String, String> body) {
        String oldPwd = body.get("old");
        String newPwd = body.get("new");
        if (newPwd == null) {
            newPwd = body.get("newPassword");
        }
        if (newPwd == null) {
            throw new BizException("新密码必填");
        }
        service.changePassword(oldPwd, newPwd);
        return R.ok();
    }
}
