package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.model.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
@Transactional
public class JwtService {
  private static final Logger log = LoggerFactory.getLogger(JwtService.class);

  @Value("${security.jwt.secret-key}")
  private String secretKey;

  @Value("${security.jwt.expiration-time}")
  private long jwtExpiration;

  public String generateToken(AppUser user) {
    return Jwts.builder()
        .signWith(getSigningKey())
        .subject(user.getEmail())
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
        .compact();
  }

  private SecretKey getSigningKey() {
    byte[] keyBytes = Decoders.BASE64.decode(secretKey);
    return Keys.hmacShaKeyFor(keyBytes);
  }

  public String extractUsername(String jwt) {
    return Jwts.parser()
        .verifyWith(getSigningKey())
        .build()
        .parseSignedClaims(jwt)
        .getPayload()
        .getSubject();
  }

  private Claims extractAllClaims(String token) {
    return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
  }

  public boolean isTokenValid(String jwt, String email, AppUser user) {
    if (!email.equals(user.getEmail())) {
      log.warn("JWT validation failed because subject does not match user email. userId={}", user.getId());
      return false;
    }
    if (!user.isEnabled()) {
      log.warn("JWT validation failed because user email is not verified. userId={}", user.getId());
      return false;
    }
    if (!user.isAccountNonLocked()) {
      log.warn("JWT validation failed because user account is disabled by admin. userId={}", user.getId());
      return false;
    }
    Claims claims = extractAllClaims(jwt);
    if (claims.getExpiration().before(new Date())) {
      log.warn("JWT validation failed because token is expired. userId={}", user.getId());
      return false;
    }
    return true;
  }
}
