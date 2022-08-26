/*
 * @author Constantin Chelban (constantink@saltedge.com)
 * Copyright (c) 2022 Salt Edge.
 */
package com.saltedge.provider.demo.tools.security;

import com.saltedge.provider.demo.controllers.api.sca.v1.model.EncryptedEntity;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Base64;

public class CryptoTools {
    private final static String AES_CBC = "AES/CBC/PKCS5Padding";
    private final static String RSA_ECB = "RSA/ECB/PKCS1Padding";

    public static String encryptAes(String data, SecretKey key) {
        try {
            byte[] aesKey = key.getEncoded();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] aesKeyHash = digest.digest(aesKey);
            byte[] iv = Arrays.copyOfRange(aesKeyHash, 0, 16);
            byte[] encryptedBytes = encryptAes(data, key.getEncoded(), iv);
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static byte[] encryptAes(String data, byte[] key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_CBC);
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
        return cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    public static String decryptAes(String data, SecretKey key) {
        try {
            byte[] aesKey = key.getEncoded();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] aesKeyHash = digest.digest(aesKey);
            byte[] iv = Arrays.copyOfRange(aesKeyHash, 0, 16);
            return new String(decryptAes(data, aesKey, iv));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] decryptAes(String encryptedData, byte[] key, byte[] iv) throws Exception {
        byte[] bytes = Base64.getMimeDecoder().decode(encryptedData);
        Cipher cipher = Cipher.getInstance(AES_CBC);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
        return cipher.doFinal(bytes);
    }

    public static byte[] encryptRsa(byte[] data, PublicKey publicKey) {
        try {
            Cipher cipher = Cipher.getInstance(RSA_ECB);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return cipher.doFinal(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new byte[0];
    }

    public static byte[] decryptRsa(String data, PrivateKey privateKey) throws Exception {
        byte[] bytes = Base64.getMimeDecoder().decode(data);
        Cipher cipher = Cipher.getInstance(RSA_ECB);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(bytes);
    }

    public static String decryptPublicRsaKey(
      EncryptedEntity encryptedData,
      PrivateKey rsaPrivateKey
    ) {
        try {
            byte[] key = decryptRsa(encryptedData.getEncryptedKey(), rsaPrivateKey);
            byte[] iv = decryptRsa(encryptedData.getEncryptedIv(), rsaPrivateKey);
            byte[] keyBytes = decryptAes(encryptedData.getEncryptedData(), key, iv);
            return new String(keyBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
