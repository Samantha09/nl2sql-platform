package com.nl2sql.common.encrypt;

/**
 * 命令行工具，用于生成 AES 密钥或加密明文。
 * 用法：
 *   生成密钥：java EncryptTool --generate-key
 *   加密：    NL2SQL_ENCRYPT_KEY=xxx java EncryptTool "明文"
 */
public class EncryptTool {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("用法：");
            System.err.println("  生成密钥：java EncryptTool --generate-key");
            System.err.println("  加密：    NL2SQL_ENCRYPT_KEY=xxx java EncryptTool \"明文\"");
            System.exit(1);
        }

        if ("--generate-key".equals(args[0])) {
            System.out.println(SecureConfigEncryptor.generateKey());
            return;
        }

        String plaintext = args[0];
        String key = SecureConfigEncryptor.getKeyFromEnv();
        System.out.println(SecureConfigEncryptor.encrypt(plaintext, key));
    }
}
