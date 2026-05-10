package com.mp.core.util;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AESUtil {
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private static String key;

    @Value("${app.security.aes-key}")
    public void setKey(String aesKey) {
        AESUtil.key = aesKey;
    }

    public static String encrypt(String data) throws Exception {
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);
        byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

        // Prepend IV to ciphertext: [IV (12 bytes)][ciphertext+tag]
        byte[] combined = ByteBuffer.allocate(iv.length + encrypted.length)
                .put(iv)
                .put(encrypted)
                .array();

        return Base64.getEncoder().encodeToString(combined);
    }

    public static String decrypt(String encryptedData) throws Exception {
        byte[] combined = Base64.getDecoder().decode(encryptedData);

        // Extract IV and ciphertext
        ByteBuffer buffer = ByteBuffer.wrap(combined);
        byte[] iv = new byte[GCM_IV_LENGTH];
        buffer.get(iv);
        byte[] ciphertext = new byte[buffer.remaining()];
        buffer.get(ciphertext);

        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
        byte[] decrypted = cipher.doFinal(ciphertext);

        return new String(decrypted, StandardCharsets.UTF_8);
    }
} 