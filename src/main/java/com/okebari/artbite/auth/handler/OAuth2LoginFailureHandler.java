package com.okebari.artbite.auth.handler;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.okebari.artbite.common.dto.CustomApiResponse;
import com.okebari.artbite.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

	private final ObjectMapper objectMapper;

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
		AuthenticationException exception) throws IOException, ServletException {
		log.debug("OAuth2 Authentication Failed: {}", exception.getMessage(), exception); // Changed to debug

		HttpStatus status = HttpStatus.UNAUTHORIZED;
		String message = "소셜 로그인에 실패했습니다. 다시 시도해주세요.";

		// CustomApiResponse를 사용하여 에러 응답 생성
		CustomApiResponse<?> errorResponse = CustomApiResponse.error(ErrorCode.COMMON_UNAUTHORIZED, message);

		response.setContentType("application/json;charset=UTF-8");
		response.setStatus(status.value());
		response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
	}
}
