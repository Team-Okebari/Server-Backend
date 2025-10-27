package com.okebari.artbite.auth.jwt;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	public static final String AUTHORIZATION_HEADER = "Authorization";
	public static final String BEARER_PREFIX = "Bearer ";

	private final JwtProvider jwtProvider;
	private final UserDetailsService userDetailsService; // UserDetailsService 주입
	private final RedisTemplate<String, String> redisTemplate; // RedisTemplate 주입

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
		FilterChain filterChain) throws ServletException, IOException {
		String jwt = resolveToken(request);

		if (StringUtils.hasText(jwt) && jwtProvider.validateToken(jwt)) {
			// Redis 블랙리스트에 있는 토큰인지 확인
			String isLogout = redisTemplate.opsForValue().get(jwt);
			if (isLogout != null && isLogout.equals("logout")) {
				// 블랙리스트에 있는 토큰이면 인증 실패 처리
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				response.getWriter().write("Expired or Invalid Token");
				return;
			}

			// 토큰에서 사용자 이메일 추출
			String email = jwtProvider.getAuthentication(jwt).getName();
			// DB에서 최신 사용자 정보 조회
			UserDetails userDetails = userDetailsService.loadUserByUsername(email);
			// 최신 정보로 인증 객체 생성
			Authentication authentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
				userDetails, null, userDetails.getAuthorities());
			// SecurityContext에 저장
			SecurityContextHolder.getContext().setAuthentication(authentication);
		}

		filterChain.doFilter(request, response);
	}

	private String resolveToken(HttpServletRequest request) {
		String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
			return bearerToken.substring(7);
		}
		return null;
	}
}
