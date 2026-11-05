package com.platform.framework.core.crypto;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 설정값(토큰·비밀번호) 암호화 — AES-256-GCM. 마스터 키에서 값마다 새 솔트로 PBKDF2(HmacSHA256)를 돌려 암호키를 만들고, 값마다 새 IV 를 쓴다.
 * 결과는 {@code ENC(base64url(버전|솔트|IV|암호문+태그))} 문자열이라 application.yml 에 그대로 적을 수 있고,
 * 같은 값을 암호화해도 매번 다른 문자열이 나온다. GCM 태그로 변조도 감지한다(복호화 실패).
 * 마스터 키는 소스에 두지 않는다(환경변수 JEPF_SECRET_KEY 또는 키 파일 — {@link MasterKey}).
 */
public final class SecretCipher {

    public static final String PREFIX = "ENC(";
    public static final String SUFFIX = ")";

    private static final int VERSION = 1;
    private static final int SALT_BYTES = 16;
    private static final int IV_BYTES = 12;
    private static final int PBKDF2_ITERATIONS = 120_000;
    private static final int KEY_BITS = 256;
    private static final int TAG_BITS = 128;

    private final SecureRandom random = new SecureRandom();
    private final char[] masterKey;

    public SecretCipher(String masterKey) {
        if (masterKey == null || masterKey.isBlank()) {
            throw new IllegalArgumentException("마스터 키가 비어 있습니다.");
        }
        this.masterKey = masterKey.toCharArray();
    }

    /** ENC(...) 형태인지. */
    public static boolean isEncrypted(String value) {
        return value != null && value.startsWith(PREFIX) && value.endsWith(SUFFIX) && value.length() > PREFIX.length() + SUFFIX.length();
    }

    public String encrypt(String plain) {
        try {
            byte[] salt = new byte[SALT_BYTES];
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(salt);
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(salt), new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(new byte[] {(byte) VERSION});
            byte[] body = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buf = ByteBuffer.allocate(1 + SALT_BYTES + IV_BYTES + body.length);
            buf.put((byte) VERSION).put(salt).put(iv).put(body);
            return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(buf.array()) + SUFFIX;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("암호화에 실패했습니다.", e);
        }
    }

    /** ENC(...) 값을 복호화한다. 마스터 키가 다르거나 값이 손상되었으면 IllegalArgumentException. */
    public String decrypt(String encrypted) {
        if (!isEncrypted(encrypted)) {
            throw new IllegalArgumentException("ENC(...) 형식이 아닙니다.");
        }
        try {
            byte[] all = Base64.getUrlDecoder().decode(encrypted.substring(PREFIX.length(), encrypted.length() - SUFFIX.length()));
            if (all.length < 1 + SALT_BYTES + IV_BYTES + TAG_BITS / 8 || all[0] != VERSION) {
                throw new IllegalArgumentException("지원하지 않는 암호문 형식입니다.");
            }
            byte[] salt = java.util.Arrays.copyOfRange(all, 1, 1 + SALT_BYTES);
            byte[] iv = java.util.Arrays.copyOfRange(all, 1 + SALT_BYTES, 1 + SALT_BYTES + IV_BYTES);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(salt), new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(new byte[] {(byte) VERSION});
            return new String(cipher.doFinal(all, 1 + SALT_BYTES + IV_BYTES, all.length - 1 - SALT_BYTES - IV_BYTES), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalArgumentException("복호화에 실패했습니다(마스터 키가 다르거나 값이 손상되었습니다).", e);
        }
    }

    private SecretKeySpec key(byte[] salt) throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(masterKey, salt, PBKDF2_ITERATIONS, KEY_BITS);
        try {
            return new SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(), "AES");
        } finally {
            spec.clearPassword();
        }
    }
}
