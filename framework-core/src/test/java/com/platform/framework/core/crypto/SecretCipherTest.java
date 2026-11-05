package com.platform.framework.core.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SecretCipherTest {

    @Test
    void 암호화한_값은_복호화하면_원래대로_돌아온다() {
        SecretCipher c = new SecretCipher("master-key-1");
        String enc = c.encrypt("9031567075:AAEV-token/한글+=");
        assertTrue(SecretCipher.isEncrypted(enc));
        assertFalse(enc.contains("9031567075"));
        assertEquals("9031567075:AAEV-token/한글+=", c.decrypt(enc));
    }

    @Test
    void 같은_값도_매번_다른_암호문이_나온다() {
        SecretCipher c = new SecretCipher("master-key-1");
        assertNotEquals(c.encrypt("same"), c.encrypt("same"));
    }

    @Test
    void 마스터_키가_다르면_복호화에_실패한다() {
        String enc = new SecretCipher("master-key-1").encrypt("secret");
        assertThrows(IllegalArgumentException.class, () -> new SecretCipher("other-key").decrypt(enc));
    }

    @Test
    void 암호문이_변조되면_복호화에_실패한다() {
        SecretCipher c = new SecretCipher("master-key-1");
        String enc = c.encrypt("secret");
        String body = enc.substring(4, enc.length() - 1);
        char last = body.charAt(body.length() - 2);
        String tampered = SecretCipher.PREFIX + body.substring(0, body.length() - 2) + (last == 'A' ? 'B' : 'A') + body.charAt(body.length() - 1) + SecretCipher.SUFFIX;
        assertThrows(IllegalArgumentException.class, () -> c.decrypt(tampered));
    }

    @Test
    void ENC_형식이_아니면_거부한다() {
        assertFalse(SecretCipher.isEncrypted("plain"));
        assertFalse(SecretCipher.isEncrypted("ENC()"));
        assertThrows(IllegalArgumentException.class, () -> new SecretCipher("k").decrypt("plain"));
    }
}
