package com.dms.common;

import com.dms.auth.AuthInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final AuthInterceptor authInterceptor;

    public WebConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Value("${dms.cors.allowed-origins:*}")
    private String allowedOrigins;

    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/captcha",
                        "/api/auth/captcha/**",
                        "/api/invoice/callback/**",
                        "/api/open/**",
                        "/sim/**",
                        "/h2/**",
                        "/error");
    }

    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(allowedOrigins.split(","))
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
