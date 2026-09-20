package com.dms.auth.controller;

import com.dms.auth.CaptchaService;
import com.dms.auth.entity.SysUser;
import com.dms.auth.service.AuthService;
import com.dms.common.BizException;
import com.dms.common.R;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService service;
    private final CaptchaService captchaService;

    public AuthController(AuthService service, CaptchaService captchaService) {
        this.service = service;
        this.captchaService = captchaService;
    }

    @PostMapping("/login")
    public R<Map<String, Object>> login(
            @RequestBody Map<String, String> body, HttpServletRequest request) {
        return R.ok(
                service.login(
                        body.get("username"),
                        body.get("password"),
                        body.get("captchaId"),
                        body.get("captchaCode"),
                        request));
    }

    @GetMapping("/captcha")
    public R<Map<String, String>> captcha() {
        return R.ok(captchaService.create());
    }

    @GetMapping("/captcha/required")
    public R<Map<String, Boolean>> captchaRequired(
            @RequestParam(required = false) String username, HttpServletRequest request) {
        Map<String, Boolean> m = new HashMap<>();
        m.put("required", service.captchaRequired(username, request));
        return R.ok(m);
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
