package com.nl2sql.common.encrypt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EncryptedPropertyEnvironmentPostProcessor - 自动解密")
class EncryptedPropertyEnvironmentPostProcessorTest {

    @Test
    @DisplayName("应将 ENC(...) 属性解密为明文")
    void shouldDecryptEncryptedProperty() {
        String key = SecureConfigEncryptor.generateKey();
        System.setProperty(SecureConfigEncryptor.ENV_KEY_NAME, key);
        try {
            String encrypted = SecureConfigEncryptor.encrypt("nl2sql123", key);

            MockEnvironment environment = new MockEnvironment()
                    .withProperty("mysql.password", encrypted)
                    .withProperty("mysql.host", "localhost");

            new EncryptedPropertyEnvironmentPostProcessor()
                    .postProcessEnvironment(environment, new SpringApplication());

            assertThat(environment.getProperty("mysql.password")).isEqualTo("nl2sql123");
            assertThat(environment.getProperty("mysql.host")).isEqualTo("localhost");
        } finally {
            System.clearProperty(SecureConfigEncryptor.ENV_KEY_NAME);
        }
    }
}
