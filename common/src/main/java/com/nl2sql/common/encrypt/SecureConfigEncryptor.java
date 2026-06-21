package com.nl2sql.common.encrypt;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 配置敏感字段 AES-256-GCM 加解密工具。
 * 密文格式：ENC(BASE64(iv || ciphertext || authTag))
 * 密钥来源：环境变量 NL2SQL_ENCRYPT_KEY（Base64 编码的 32 字节 AES 密钥）
 */
public final class SecureConfigEncryptor {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    private static final int AES_KEY_LENGTH = 32;

    static final String ENV_KEY_NAME = "NL2SQL_ENCRYPT_KEY";
    private static final String PREFIX = "ENC(";
    private static final String SUFFIX = ")";

    private SecureConfigEncryptor() {
    }

    /** 从环境变量读取 Base64 编码的 AES 密钥 */
    public static String getKeyFromEnv() {
        String key = System.getenv(ENV_KEY_NAME);
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("环境变量 " + ENV_KEY_NAME + " 未设置，无法解密 ENC(...) 配置");
        }
        return key;
    }

    /** 生成新的 32 字节 AES 密钥并 Base64 编码 */
    public static String generateKey() {
        byte[] key = new byte[AES_KEY_LENGTH];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }

    /** 判断字符串是否为 ENC(...) 格式 */
    public static boolean isEncrypted(String value) {
        return value != null && value.startsWith(PREFIX) && value.endsWith(SUFFIX);
    }

    /** 加密明文，返回 ENC(...) 格式 */
    public static String encrypt(String plaintext, String base64Key) {
        if (plaintext == null) {
            return null;
        }
        byte[] keyBytes = decodeKey(base64Key);
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            SecretKey secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            String encoded = Base64.getEncoder().encodeToString(byteBuffer.array());
            return PREFIX + encoded + SUFFIX;
        } catch (Exception e) {
            throw new IllegalStateException("AES-256-GCM 加密失败", e);
        }
    }

    /** 解密 ENC(...) 格式密文 */
    public static String decrypt(String encrypted, String base64Key) {
        if (encrypted == null) {
            return null;
        }
        if (!isEncrypted(encrypted)) {
            return encrypted;
        }

        String payload = encrypted.substring(PREFIX.length(), encrypted.length() - SUFFIX.length());
        byte[] decoded = Base64.getDecoder().decode(payload);

        if (decoded.length < GCM_IV_LENGTH + GCM_TAG_LENGTH) {
            throw new IllegalArgumentException("密文格式非法，长度不足");
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
        byte[] iv = new byte[GCM_IV_LENGTH];
        byteBuffer.get(iv);
        byte[] cipherText = new byte[byteBuffer.remaining()];
        byteBuffer.get(cipherText);

        byte[] keyBytes = decodeKey(base64Key);
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            SecretKey secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("AES-256-GCM 解密失败，请检查 NL2SQL_ENCRYPT_KEY 是否正确", e);
        }
    }

    private static byte[] decodeKey(String base64Key) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        if (keyBytes.length != AES_KEY_LENGTH) {
            throw new IllegalArgumentException("AES 密钥必须是 32 字节，当前 " + keyBytes.length + " 字节");
        }
        return keyBytes;
    }
}
