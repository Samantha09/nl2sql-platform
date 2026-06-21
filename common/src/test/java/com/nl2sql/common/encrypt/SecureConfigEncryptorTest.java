package com.nl2sql.common.encrypt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SecureConfigEncryptor - AES-256-GCM 加解密")
class SecureConfigEncryptorTest {

    @Test
    @DisplayName("加解密应可逆")
    void shouldEncryptAndDecrypt() {
        String key = SecureConfigEncryptor.generateKey();
        String encrypted = SecureConfigEncryptor.encrypt("nl2sql123", key);

        assertThat(encrypted).startsWith("ENC(").endsWith(")");
        assertThat(SecureConfigEncryptor.decrypt(encrypted, key)).isEqualTo("nl2sql123");
    }

    @Test
    @DisplayName("相同明文两次加密结果应不同")
    void shouldUseRandomIv() {
        String key = SecureConfigEncryptor.generateKey();
        String encrypted1 = SecureConfigEncryptor.encrypt("nl2sql123", key);
        String encrypted2 = SecureConfigEncryptor.encrypt("nl2sql123", key);

        assertThat(encrypted1).isNotEqualTo(encrypted2);
    }

    @Test
    @DisplayName("错误密钥解密应失败")
    void shouldFailWithWrongKey() {
        String key = SecureConfigEncryptor.generateKey();
        String wrongKey = SecureConfigEncryptor.generateKey();
        String encrypted = SecureConfigEncryptor.encrypt("nl2sql123", key);

        assertThatThrownBy(() -> SecureConfigEncryptor.decrypt(encrypted, wrongKey))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("非 ENC 字符串应原样返回")
    void shouldReturnPlainValue() {
        assertThat(SecureConfigEncryptor.decrypt("plaintext", SecureConfigEncryptor.generateKey()))
                .isEqualTo("plaintext");
    }

    @Test
    @DisplayName("null 明文加密应返回 null")
    void shouldHandleNullPlaintext() {
        assertThat(SecureConfigEncryptor.encrypt(null, SecureConfigEncryptor.generateKey())).isNull();
    }
}
