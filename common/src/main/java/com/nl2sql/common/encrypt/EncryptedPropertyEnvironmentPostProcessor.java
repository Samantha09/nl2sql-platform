package com.nl2sql.common.encrypt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 在 Environment 准备完成后扫描所有 PropertySource，
 * 将 ENC(...) 格式的密文解密为明文，并置于最高优先级。
 */
@Order(Ordered.LOWEST_PRECEDENCE)
public class EncryptedPropertyEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Pattern ENC_PATTERN = Pattern.compile("^ENC\\((.+)\\)$");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        boolean hasEncryptedValue = false;
        Map<String, Object> decrypted = new HashMap<>();

        for (PropertySource<?> source : environment.getPropertySources()) {
            if (source instanceof EnumerablePropertySource<?> enumerableSource) {
                for (String name : enumerableSource.getPropertyNames()) {
                    Object value = enumerableSource.getProperty(name);
                    if (value instanceof String str && SecureConfigEncryptor.isEncrypted(str)) {
                        hasEncryptedValue = true;
                        Matcher matcher = ENC_PATTERN.matcher(str);
                        if (matcher.matches()) {
                            String key = resolveKey();
                            decrypted.put(name, SecureConfigEncryptor.decrypt(str, key));
                        }
                    }
                }
            }
        }

        if (hasEncryptedValue && decrypted.isEmpty()) {
            throw new IllegalStateException("存在 ENC(...) 密文配置但解密失败，请检查 NL2SQL_ENCRYPT_KEY 环境变量");
        }

        if (!decrypted.isEmpty()) {
            MapPropertySource decryptedSource = new MapPropertySource("decrypted-properties", decrypted);
            environment.getPropertySources().addFirst(decryptedSource);
        }
    }

    private String resolveKey() {
        String key = System.getenv(SecureConfigEncryptor.ENV_KEY_NAME);
        if (key == null || key.isBlank()) {
            key = System.getProperty(SecureConfigEncryptor.ENV_KEY_NAME);
        }
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("环境变量 " + SecureConfigEncryptor.ENV_KEY_NAME + " 未设置，无法解密 ENC(...) 配置");
        }
        return key;
    }
}
