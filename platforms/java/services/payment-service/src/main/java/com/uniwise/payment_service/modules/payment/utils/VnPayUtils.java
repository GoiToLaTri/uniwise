package com.uniwise.payment_service.modules.payment.utils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class VnPayUtils {

    /**
     * Tính toán HMAC-SHA512 từ key và data.
     */
    public static String hmacSHA512(String key, String data) {
        try {
            if (key == null || data == null) {
                throw new NullPointerException("key and data must not be null");
            }
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac512.init(secretKey);
            byte[] result = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception ex) {
            return "";
        }
    }

    /**
     * Tạo chuỗi query string để gửi lên VNPay (khi tạo link thanh toán).
     * Keys không encode, Values được URL-encode bằng UTF-8.
     * Đây là chuẩn của VNPay Java SDK chính thức.
     */
    public static String buildQueryString(Map<String, String> fields) {
        Map<String, String> sortedFields = new TreeMap<>(fields);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : sortedFields.entrySet()) {
            String fieldName = entry.getKey();
            String fieldValue = entry.getValue();
            if (fieldValue != null && !fieldValue.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append("&");
                }
                // Keys KHÔNG được encode, chỉ encode values
                sb.append(fieldName);
                sb.append("=");
                sb.append(encodeValue(fieldValue));
            }
        }
        return sb.toString();
    }

    /**
     * Tạo chuỗi hash để xác thực chữ ký từ IPN callback.
     * VNPay gửi các tham số đã ĐƯỢC decode sẵn, nên cần encode lại trước khi hash.
     * Keys KHÔNG được encode, Values được URL-encode bằng UTF-8.
     */
    public static String buildHashData(Map<String, String> fields) {
        return buildQueryString(fields); // Cùng quy tắc: key không encode, value encode
    }

    /**
     * URL-encode giá trị theo chuẩn UTF-8.
     * Giữ nguyên dấu '+' (khoảng trắng) — KHÔNG thay bằng '%20'.
     * VNPay dùng URLEncoder Java chuẩn nên khoảng trắng phải là '+', không phải '%20'.
     */
    public static String encodeValue(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            return "";
        }
    }
}
