package com.example.Elderly_care_Platfrom.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JwtUtil {
    //生成 token 和解析 token 用的是密钥
    private static final String SECRET = "care-system-jwt-secret-key-2025-spring-boot-4";
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    // token 过期时间
    private static final long EXPIRATION =  30 * 60 * 1000L; // 30分钟

     // 生成 token
    public static String generateToken(String userId, String role, String name) {
        return Jwts.builder()
                .claim("userId", userId)
                .claim("role", role)
                .claim("name", name)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(KEY)
                .compact();
    }
    // 解析 token
    public static Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
