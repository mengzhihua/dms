package com.dms.auth;

import java.util.Arrays;
import java.util.List;
import org.springframework.util.AntPathMatcher;

/** 角色 → 接口路径权限矩阵。读 = GET/HEAD/OPTIONS；其余视为写。 */
public class RolePolicy {
    private static final AntPathMatcher M = new AntPathMatcher();

    /** OEM 可写：网络/维修指导/车型/备件主数据/调研模板题目。 */
    private static final List<String> OEM_WRITE =
            Arrays.asList(
                    "/api/network/**",
                    "/api/guide/**",
                    "/api/customer/model/**",
                    "/api/parts/part/**",
                    "/api/survey/template/**",
                    "/api/survey/question/**");

    /** 经销商经理禁止写：经销商主数据、目标、考核、指导库主数据、调研模板、用户管理。 */
    private static final List<String> MGR_DENY_WRITE =
            Arrays.asList(
                    "/api/network/dealer/**",
                    "/api/network/direct-store/**",
                    "/api/network/target/**",
                    "/api/network/assessment/**",
                    "/api/guide/labor/**",
                    "/api/guide/guide/**",
                    "/api/guide/bulletin/**",
                    "/api/survey/template/**",
                    "/api/survey/question/**",
                    "/api/auth/user/**");

    /** 服务顾问可写：工单全流程、客户车辆、调研记录/答卷/投诉、备件预留释放、OMS 补货。 */
    private static final List<String> ADVISOR_WRITE =
            Arrays.asList(
                    "/api/workshop/**",
                    "/api/customer/**",
                    "/api/survey/record/**",
                    "/api/survey/*/answer",
                    "/api/survey/complaint/**",
                    "/api/parts/stock/reserve**",
                    "/api/parts/stock/release**",
                    "/api/parts/stock/inbound",
                    "/api/oms/replenish/**");

    /** 技师只读范围。 */
    private static final List<String> TECH_READ =
            Arrays.asList(
                    "/api/workshop/**",
                    "/api/guide/**",
                    "/api/customer/**",
                    "/api/parts/**",
                    "/api/network/dealer/**",
                    "/api/dashboard/**");
    /** 技师可写：工单开工/完工/质检。 */
    private static final List<String> TECH_WRITE =
            Arrays.asList(
                    "/api/workshop/order/*/start",
                    "/api/workshop/order/*/finish",
                    "/api/workshop/order/*/qc");

    /** 财务可写：发票全流程、工单结算、整车销售开票/交车。 */
    private static final List<String> FIN_WRITE =
            Arrays.asList(
                    "/api/invoice/**",
                    "/api/workshop/order/*/settle",
                    "/api/network/sales-order/*/invoice",
                    "/api/network/sales-order/*/deliver");

    private RolePolicy() {}

    private static boolean read(String method) {
        return "GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method);
    }

    private static boolean any(List<String> patterns, String path) {
        for (String p : patterns) {
            if (M.match(p, path)) {
                return true;
            }
        }
        return false;
    }

    /** 所有登录用户均可：个人信息、登出、改密码。 */
    private static final List<String> AUTHENTICATED_ANY =
            Arrays.asList("/api/auth/me", "/api/auth/logout", "/api/auth/password");

    public static boolean allowed(String roleName, String method, String path) {
        Role role = Role.of(roleName);
        if (role == null) {
            return false;
        }
        if (any(AUTHENTICATED_ANY, path)) {
            return true;
        }
        switch (role) {
            case ADMIN:
                return true;
            case OEM:
                return read(method) || any(OEM_WRITE, path);
            case DEALER_MANAGER:
                return read(method) || !any(MGR_DENY_WRITE, path);
            case ADVISOR:
                return read(method) || any(ADVISOR_WRITE, path);
            case TECHNICIAN:
                return read(method) ? any(TECH_READ, path) : any(TECH_WRITE, path);
            case FINANCE:
                return read(method) || any(FIN_WRITE, path);
            default:
                return false;
        }
    }
}
