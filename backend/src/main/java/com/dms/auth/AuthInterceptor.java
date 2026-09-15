package com.dms.auth;

import com.dms.common.R;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    private final JwtService jwtService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuthInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler)
            throws Exception {
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            return true;
        }
        String header = req.getHeader("Authorization");
        LoginUser user = null;
        if (header != null && header.startsWith("Bearer ")) {
            user = jwtService.parse(header.substring(7));
        }
        if (user == null) {
            write(res, 401, "未登录或登录已过期");
            return false;
        }
        UserContext.set(user);
        String path = req.getRequestURI();
        if (!RolePolicy.allowed(user.getRole().name(), req.getMethod(), path)) {
            write(res, 403, "无权限");
            return false;
        }
        return true;
    }

    public void afterCompletion(
            HttpServletRequest req, HttpServletResponse res, Object handler, Exception ex) {
        UserContext.clear();
    }

    private void write(HttpServletResponse res, int code, String msg) throws Exception {
        res.setStatus(code);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write(objectMapper.writeValueAsString(R.fail(code, msg)));
    }
}
