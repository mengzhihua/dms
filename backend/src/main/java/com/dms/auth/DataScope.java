package com.dms.auth;

import com.dms.common.BizException;

/** 经销商数据范围工具：非全网角色只能访问/操作本经销商数据。 */
public class DataScope {
    private DataScope() {}

    /** 校验 dealerCode 属于当前用户数据范围；越权抛 400。 */
    public static void check(String dealerCode) {
        LoginUser u = UserContext.get();
        if (u == null || u.networkWide()) {
            return;
        }
        if (dealerCode == null || !dealerCode.equals(u.getDealerCode())) {
            throw new BizException("无权访问其他经销商数据");
        }
    }

    /** 非全网用户强制返回自身经销商；未登录/全网用户返回原参数。 */
    public static String effectiveDealer(String dealerCode) {
        LoginUser u = UserContext.get();
        if (u == null || u.networkWide()) {
            return dealerCode;
        }
        return u.getDealerCode();
    }
}
