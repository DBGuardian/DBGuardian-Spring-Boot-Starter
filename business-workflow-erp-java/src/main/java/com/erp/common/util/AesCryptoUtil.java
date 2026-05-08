package com.erp.common.util;

import cn.hutool.core.util.StrUtil;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;

/**
 * AES-256 加解密工具
 *
 * @author ERP
 */
public final class AesCryptoUtil {

    private static final String AES_TRANSFORMATION = "AES/ECB/PKCS5Padding";
    private static final int AES_256_KEY_LENGTH = 32;

    private AesCryptoUtil() {
    }

    /**
     * AES加密
     *
     * @param plainText 明文
     * @param rawKey    32位密钥
     * @return 密文
     */
    public static String encrypt(String plainText, String rawKey) {
        if (StrUtil.isBlank(plainText)) {
            throw new IllegalArgumentException("明文不能为空");
        }
        try {
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, buildKey(rawKey));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("AES加密失败", ex);
        }
    }

    /**
     * AES解密
     *
     * @param cipherText 密文
     * @param rawKey     32位密钥
     * @return 明文
     */
    public static String decrypt(String cipherText, String rawKey) {
        if (StrUtil.isBlank(cipherText)) {
            throw new IllegalArgumentException("密文不能为空");
        }
        try {
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, buildKey(rawKey));
            byte[] decoded = Base64.getDecoder().decode(cipherText);
            byte[] plainBytes = cipher.doFinal(decoded);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("AES解密失败", ex);
        }
    }

    private static SecretKeySpec buildKey(String rawKey) {
        if (StrUtil.isBlank(rawKey) || rawKey.length() != AES_256_KEY_LENGTH) {
            throw new IllegalArgumentException("AES密钥必须为32位");
        }
        byte[] keyBytes = rawKey.getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(keyBytes, "AES");
    }
}






























