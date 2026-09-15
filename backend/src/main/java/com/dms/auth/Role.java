package com.dms.auth;

public enum Role {
    ADMIN,
    OEM,
    DEALER_MANAGER,
    ADVISOR,
    TECHNICIAN,
    FINANCE;

    /** 全网角色：不受经销商数据范围限制。 */
    public boolean networkWide() {
        return this == ADMIN || this == OEM;
    }

    public static Role of(String name) {
        try {
            return Role.valueOf(name);
        } catch (Exception e) {
            return null;
        }
    }
}
