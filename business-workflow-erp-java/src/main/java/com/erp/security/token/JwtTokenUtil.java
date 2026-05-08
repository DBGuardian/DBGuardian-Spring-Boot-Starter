package com.erp.security.token;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Token工具类
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Slf4j
@Component
public class JwtTokenUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;

    /**
     * 生成Token
     */
    public String generateToken(String username, Integer userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);
        claims.put("userId", userId);
        return createToken(claims, username, expiration);
    }

    /**
     * 生成刷新Token
     */
    public String generateRefreshToken(String username, Integer userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);
        claims.put("userId", userId);
        return createToken(claims, username, refreshExpiration);
    }

    /**
     * 创建Token
     */
    private String createToken(Map<String, Object> claims, String subject, Long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        // 确保密钥长度至少32字节（256位）用于HS512算法
        SecretKey key = getSecretKey();

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * 获取SecretKey，确保密钥长度足够
     * HS512算法需要至少64字节（512位）的密钥
     */
    private SecretKey getSecretKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        
        // HS512算法需要至少64字节（512位）的密钥
        // 如果密钥长度不足，使用SHA-256哈希来扩展密钥
        if (keyBytes.length < 64) {
            try {
                java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
                byte[] extended = new byte[64];
                
                // 如果原始密钥长度不足64字节，使用SHA-256哈希来扩展
                if (keyBytes.length < 32) {
                    // 密钥太短，先哈希一次
                    byte[] hashed = digest.digest(keyBytes);
                    System.arraycopy(hashed, 0, extended, 0, 32);
                    // 再次哈希并填充剩余部分
                    byte[] secondHash = digest.digest(hashed);
                    System.arraycopy(secondHash, 0, extended, 32, 32);
                } else {
                    // 密钥长度在32-64之间，直接复制并哈希填充
                    System.arraycopy(keyBytes, 0, extended, 0, keyBytes.length);
                    byte[] padding = digest.digest(keyBytes);
                    System.arraycopy(padding, 0, extended, keyBytes.length, 64 - keyBytes.length);
                }
                
                return Keys.hmacShaKeyFor(extended);
            } catch (java.security.NoSuchAlgorithmException e) {
                throw new RuntimeException("无法创建密钥", e);
            }
        }
        
        // 如果密钥长度已经足够，直接使用
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 从Token中获取Claims
     * 
     * @param token JWT Token
     * @return Claims对象，如果Token过期或无效则返回null
     */
    public Claims getClaimsFromToken(String token) {
        try {
            SecretKey key = getSecretKey();
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.warn("JWT Token已过期: {}", e.getMessage());
            return null;
        } catch (MalformedJwtException e) {
            log.warn("JWT Token格式错误: {}", e.getMessage());
            return null;
        } catch (UnsupportedJwtException e) {
            log.warn("不支持的JWT Token: {}", e.getMessage());
            return null;
        } catch (IllegalArgumentException e) {
            log.warn("JWT Token为空: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("解析JWT Token异常", e);
            return null;
        }
    }

    /**
     * 从Token中获取用户名
     * 
     * @param token JWT Token
     * @return 用户名，如果Token无效或过期则返回null
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.getSubject() : null;
    }

    /**
     * 从Token中获取用户ID
     * 
     * @param token JWT Token
     * @return 用户ID，如果Token无效或过期则返回null
     */
    public Integer getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.get("userId", Integer.class) : null;
    }

    /**
     * 验证Token是否过期
     * 
     * @param token JWT Token
     * @return true表示过期，false表示未过期
     */
    public Boolean isTokenExpired(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            if (claims == null) {
                return true;
            }
            Date expiration = claims.getExpiration();
            return expiration != null && expiration.before(new Date());
        } catch (Exception e) {
            log.warn("验证Token过期状态异常", e);
            return true;
        }
    }
    
    /**
     * 检查Token是否过期（不抛出异常）
     * 
     * @param token JWT Token
     * @return true表示过期，false表示未过期或Token无效
     */
    public Boolean checkTokenExpired(String token) {
        try {
            SecretKey key = getSecretKey();
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            Date expiration = claims.getExpiration();
            return expiration != null && expiration.before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            log.warn("检查Token过期状态异常", e);
            return true;
        }
    }

    /**
     * 验证Token是否有效
     */
    public Boolean validateToken(String token, String username) {
        try {
            if (token == null || username == null) {
                return false;
            }
            String tokenUsername = getUsernameFromToken(token);
            return tokenUsername != null && tokenUsername.equals(username) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
}





















