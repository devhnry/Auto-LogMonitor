package org.remita.autologmonitor.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.remita.autologmonitor.dto.UserClaimsDto;
import org.remita.autologmonitor.entity.BaseUserEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.function.Function;

@Service
public class JWTService {

    private SecretKey key;
    private byte[] keyByte;

    public JWTService() {
        String secretString = "HFUKNSF09839IKSFV9348J39GHIUEWFJR9EF089WUJ4FNR9HG738UJW4ONEMSCO";
        keyByte = Base64.getDecoder().decode(secretString.getBytes(StandardCharsets.UTF_8));
        this.key = new SecretKeySpec(keyByte, "HmacSHA256");
    }

    public String generateToken(BaseUserEntity user){
        return Jwts.builder()
                .claim("orgId", user.getOrganization().getOrganizationId())
                .claim("user", new UserClaimsDto(user.getId(),user.getFirstname(),user.getEmail()))
                .claim("roles", user.getAuthorities())
                .claim("type", "BEARER")
                .setSubject(user.getUsername())
                .setIssuedAt(Date.from(Instant.now()))
                .setExpiration(Date.from(Instant.now().plus(24, ChronoUnit.HOURS)))
                .signWith(SignatureAlgorithm.valueOf("HmacSHA256"), keyByte)
                .compact();
    }

    public String generateRefreshToken(HashMap<String, Object> claims, UserDetails userDetails){
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(Date.from(Instant.now()))
                .setExpiration(Date.from(Instant.now().plus(24, ChronoUnit.HOURS)))
                .signWith(SignatureAlgorithm.valueOf("HmacSHA256"), keyByte)
                .compact();
    }

    public <T> T extractClaims(String token, Function<Claims, T> claimsTFunction){
        return claimsTFunction.apply(Jwts.parser().setSigningKey(key).parseClaimsJwt(token).getBody());
    }

    public String extractUsername(String token){
        return extractClaims(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, UserDetails userDetails){
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    public boolean isTokenExpired(String token) {
        return extractClaims(token, Claims::getExpiration).before(new Date());
    }
}

