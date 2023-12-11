package org.systemli.keycloak;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordVerifierTest {

    private PasswordVerifier passwordVerifier;

    public PasswordVerifierTest() {
        passwordVerifier = new PasswordVerifier();
    }

    @Test
    void testVerify() {
        String password = "password";
        String hash = "$argon2id$v=19$m=65536,t=4,p=1$Exe5ShJPEy6Ar4tFMTmIzg$exP1vf3XaWSXGkboeGfEGhbfly9kMrdl4BIzPKQWY/E";

        assertTrue(passwordVerifier.verify(password, hash));
    }

    @Test
    void testVerifyWrongPassword() {
        String password = "wrongpassword";
        String hash = "$argon2id$v=19$m=65536,t=4,p=1$Exe5ShJPEy6Ar4tFMTmIzg$exP1vf3XaWSXGkboeGfEGhbfly9kMrdl4BIzPKQWY/E";

        assertFalse(passwordVerifier.verify(password, hash));
    }
}
