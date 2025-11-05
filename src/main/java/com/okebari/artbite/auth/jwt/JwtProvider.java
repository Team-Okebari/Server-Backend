package com.okebari.artbite.auth.jwt;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.okebari.artbite.auth.service.RefreshTokenService;
import com.okebari.artbite.common.exception.InvalidTokenException;
import com.okebari.artbite.common.exception.TokenExpiredException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JwtProvider {

	private static final String AUTHORITIES_KEY = "auth";
	private final SecretKey key;
	@Getter
	private final RefreshTokenService refreshTokenService;
	@Value("${jwt.access-token-expire-time}")
	private long accessTokenExpireTime;
	@Value("${jwt.refresh-token-expire-time}")
	private long refreshTokenExpireTime;

	public JwtProvider(@Value("${jwt.secret}") String secretKey, RefreshTokenService refreshTokenService) {
		byte[] keyBytes = Decoders.BASE64.decode(secretKey);
		this.key = Keys.hmacShaKeyFor(keyBytes);
		this.refreshTokenService = refreshTokenService;
	}

	public String createToken(Authentication authentication) {
		String authorities = authentication.getAuthorities().stream()
			.map(GrantedAuthority::getAuthority)
			.collect(Collectors.joining(","));

		long now = (new Date()).getTime();
		Date validity = new Date(now + accessTokenExpireTime);

		return Jwts.builder()
			.subject(authentication.getName())
			.claim(AUTHORITIES_KEY, authorities)
			.expiration(validity)
			.signWith(key)
			.compact();
	}

	public Authentication getAuthentication(String accessToken) {
		Claims claims = parseClaims(accessToken);

		if (claims.get(AUTHORITIES_KEY) == null) {
			throw new RuntimeException("권한 정보가 없는 토큰입니다.");
		}

		Collection<? extends GrantedAuthority> authorities =
			Arrays.stream(claims.get(AUTHORITIES_KEY).toString().split(","))
				.map(SimpleGrantedAuthority::new)
				.collect(Collectors.toList());

		UserDetails principal = new User(claims.getSubject(), "", authorities);
		return new UsernamePasswordAuthenticationToken(principal, "", authorities);
	}

	public void validateToken(String token) {
		try {
			Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
		} catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
			log.debug("잘못된 JWT 서명입니다.");
			throw new InvalidTokenException("잘못된 JWT 서명입니다.");
		} catch (ExpiredJwtException e) {
			log.debug("만료된 JWT 토큰입니다.");
			throw new TokenExpiredException("만료된 JWT 토큰입니다.");
		} catch (UnsupportedJwtException e) {
			log.debug("지원되지 않는 JWT 토큰입니다.");
			throw new InvalidTokenException("지원되지 않는 JWT 토큰입니다.");
		} catch (IllegalArgumentException e) {
			log.debug("JWT 토큰이 잘못되었습니다.");
			throw new InvalidTokenException("JWT 토큰이 잘못되었습니다.");
		}
	}

	public void validateRefreshToken(String refreshToken) {
		try {
			Jwts.parser().verifyWith(key).build().parseSignedClaims(refreshToken);
		} catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
			log.debug("잘못된 Refresh Token 서명입니다.");
			throw new InvalidTokenException("잘못된 Refresh Token 서명입니다.");
		} catch (ExpiredJwtException e) {
			log.debug("만료된 Refresh Token입니다.");
			throw new TokenExpiredException("만료된 Refresh Token입니다.");
		} catch (UnsupportedJwtException e) {
			log.debug("지원되지 않는 Refresh Token입니다.");
			throw new InvalidTokenException("지원되지 않는 Refresh Token입니다.");
		} catch (IllegalArgumentException e) {
			log.debug("Refresh Token이 잘못되었습니다.");
			throw new InvalidTokenException("Refresh Token이 잘못되었습니다.");
		}
	}

	public Authentication getAuthenticationFromRefreshToken(String refreshToken) {
		Claims claims = parseClaims(refreshToken); // 만료된 경우에도 클레임을 가져오기 위해 parseClaims 메서드 사용

		if (claims.get(AUTHORITIES_KEY) == null) {
			throw new RuntimeException("권한 정보가 없는 Refresh Token입니다.");
		}

		Collection<? extends GrantedAuthority> authorities =
			Arrays.stream(claims.get(AUTHORITIES_KEY).toString().split(","))
				.map(SimpleGrantedAuthority::new)
				.collect(Collectors.toList());

		UserDetails principal = new User(claims.getSubject(), "", authorities);
		return new UsernamePasswordAuthenticationToken(principal, "", authorities);
	}

	private Claims parseClaims(String token) {
		try {
			return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
		} catch (ExpiredJwtException e) {
			return e.getClaims();
		}
	}

	public Long getExpiration(String token) {
		// parseClaims 메소드를 사용하여 만료된 토큰의 경우에도 Claims를 가져올 수 있도록 함
		Claims claims = parseClaims(token);
		return claims.getExpiration().getTime() - System.currentTimeMillis();
	}

	public long getAccessTokenExpireTime() {
		return accessTokenExpireTime;
	}

	public long getRefreshTokenExpireTime() {
		return refreshTokenExpireTime;
	}

}
