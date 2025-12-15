package com.hazergu.backend.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    /**
     * 获取密钥
     */
    private Key getSigningKey() {
        // 注意：确保 secret 至少为 256 位（32个字符）以获得更强的安全性
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * 生成 JWT 令牌
     */
    public String generateToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(username) // 新版API: .subject() 替代 .setSubject()
                .issuedAt(now)     // 新版API: .issuedAt() 替代 .setIssuedAt()
                .expiration(expiryDate) // 新版API: .expiration() 替代 .setExpiration()
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 从令牌中获取用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser() // 新版API: .parser() 替代 .parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    /**
     * 验证令牌是否有效
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser() // 新版API: .parser() 替代 .parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}