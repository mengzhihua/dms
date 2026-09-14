package com.dms.auth;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class JwtServiceTest {
    private static final String SECRET = "test-secret-key-for-dms-jwt-signing-0123456789abcdef";

    @Test
    void roundTrip() {
        JwtService s = new JwtService(SECRET, 12);
        LoginUser in = new LoginUser(7L, "d001sa", Role.ADVISOR, "D001");
        String token = s.issue(in);
        LoginUser out = s.parse(token);
        assertNotNull(out);
        assertEquals(7L, out.getId());
        assertEquals("d001sa", out.getUsername());
        assertEquals(Role.ADVISOR, out.getRole());
        assertEquals("D001", out.getDealerCode());
        assertFalse(out.networkWide());
    }

    @Test
    void expiredTokenRejected() {
        JwtService s = new JwtService(SECRET, -1);
        String token = s.issue(new LoginUser(1L, "admin", Role.ADMIN, null));
        assertNull(s.parse(token));
    }

    @Test
    void tamperedTokenRejected() {
        JwtService s = new JwtService(SECRET, 12);
        String token = s.issue(new LoginUser(1L, "admin", Role.ADMIN, null));
        assertNull(s.parse(token.substring(0, token.length() - 3) + "xyz"));
        assertNull(s.parse("garbage"));
        assertNull(new JwtService("another-secret-key-0123456789abcdef0123", 12).parse(token));
    }
}
