package com.uniwise.common.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE) // Ngăn khởi tạo instance
public class TokenUtils {

    private static final String HASH_ALGORITHM = "SHA-256";

    /**
     * Băm chuỗi token đầu vào bằng thuật toán SHA-256.
     * Kết quả trả về là chuỗi Hexadecimal để lưu vào DB.
     */
    public static String hash(String token) {
        if (token == null)
            return null;
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] encodedHash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encodedHash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 là thuật toán tiêu chuẩn luôn có sẵn trong JRE
            throw new RuntimeException("Error: Hashing algorithm not found", e);
        }
    }

    /**
     * Kiểm tra nếu token đã được băm (hash) có khớp với token gốc hay không.
     * So sánh bằng cách băm token gốc và đối chiếu với token đã băm.
     */
    public static boolean verify(String rawToken, String hashedToken) {
        if (rawToken == null || hashedToken == null)
            return false;
        String rawTokenHash = hash(rawToken);
        return rawTokenHash.equals(hashedToken);
    }

    /**
     * Tạo một chuỗi token ngẫu nhiên (dùng làm Refresh Token)
     */
    public static String generateRandomToken() {
        return UUID.randomUUID().toString();
    }

    /**
     * Helper: Chuyển mảng byte sang định dạng Hex
     */
    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
