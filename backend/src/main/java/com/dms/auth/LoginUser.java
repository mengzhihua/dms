package com.dms.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser {
    private Long id;
    private String username;
    private Role role;
    private String dealerCode;

    public boolean networkWide() {
        return role != null && role.networkWide();
    }
}
