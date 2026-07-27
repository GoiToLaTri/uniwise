package com.uniwise.jwt_security_starter;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.UnsupportedJwtException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class JwtService {
    JwtProperties jwtProperties;
    ResourceLoader resourceLoader;

    // Cache public key để tránh đọc file mỗi lần
    @NonFinal
    private PublicKey cachedPublicKey;

    public Claims extractClaims(String token) {
        try {
            PublicKey publicKey = getPublicKey();
            return verify(token, publicKey);
        } catch (Exception e) {
            log.error("Cannot extract claims from token", e);
            throw new RuntimeException("Invalid JWT token", e);
        }
    }

    private Claims verify(String token, PublicKey key) {
        Jws<Claims> signedClaims = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(jwtProperties.getIssuer())
                .requireAudience(jwtProperties.getAudience())
                .build()
                .parseSignedClaims(token);
        if (!Jwts.SIG.RS256.getId().equals(signedClaims.getHeader().getAlgorithm()))
            throw new UnsupportedJwtException("Gateway JWT must use RS256");
        return signedClaims.getPayload();
    }   

    private PublicKey loadPublicKeyFromStream(InputStream inputStream) throws Exception {
        // 1. Đọc toàn bộ nội dung từ InputStream thành String
        String keyContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

        // 2. Làm sạch chuỗi PEM:
        // - Loại bỏ Header: -----BEGIN PUBLIC KEY-----
        // - Loại bỏ Footer: -----END PUBLIC KEY-----
        // - Loại bỏ tất cả các ký tự xuống dòng (\n, \r) hoặc khoảng trắng
        String publicKeyPEM = keyContent
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replaceAll(System.lineSeparator(), "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", ""); // Loại bỏ tất cả khoảng trắng dư thừa

        // 3. Giải mã Base64 để lấy mảng byte
        byte[] encoded = Base64.getDecoder().decode(publicKeyPEM);

        // 4. Tạo đối tượng PublicKey thông qua X509EncodedKeySpec (chuẩn cho Public
        // Key)
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        return keyFactory.generatePublic(keySpec);
    }

    private PublicKey getPublicKey() throws Exception {
        if (cachedPublicKey != null) {
            return cachedPublicKey;
        }

        synchronized (this) {
            if (cachedPublicKey == null) {
                String path = jwtProperties.getPublicKeyPath();
                Resource resource = resourceLoader.getResource(path);

                try (InputStream inputStream = resource.getInputStream()) {
                    cachedPublicKey = loadPublicKeyFromStream(inputStream);
                }
            }
            return cachedPublicKey;
        }
    }
}
