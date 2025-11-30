package com.example.AuthService.security.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import com.example.AuthService.entity.User;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties props;
    private SecretKey key;

    @PostConstruct
    void initKey() {
        // props.secret là Base64
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(props.getSecret()));
    }

    public String generateAccessToken(UserDetails userDetails) {

        User user = (User) userDetails; // ⭐ Lấy đúng User của bạn

        var now = Instant.now();
        var roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return Jwts.builder()
                .claim("roles", roles)
                .claim("userId", user.getId())  // ⭐ THÊM USER ID Ở ĐÂY
                .subject(user.getUsername().toLowerCase(Locale.ROOT))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(props.getAccessExpirationMs())))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }


    public String generateRefreshToken(String username) {
        var now = Instant.now();
        return Jwts.builder()
                .subject(username.toLowerCase(Locale.ROOT))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(props.getRefreshExpirationMs())))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload().getSubject();
    }

    public boolean isValid(String token) {
        var claims = Jwts.parser()
                .clockSkewSeconds(60) // chấp nhận lệch clock nhỏ
                .verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
        return claims.getExpiration().after(new Date());
    }
}
