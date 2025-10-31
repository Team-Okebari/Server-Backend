package com.okebari.artbite.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.okebari.artbite.auth.dto.LoginRequestDto;
import com.okebari.artbite.auth.dto.SignupRequestDto;
import com.okebari.artbite.auth.dto.TokenDto;
import com.okebari.artbite.auth.service.AuthService;
import com.okebari.artbite.auth.vo.CustomUserDetails;
import com.okebari.artbite.common.dto.CustomApiResponse;
import com.okebari.artbite.common.exception.BusinessException;
import com.okebari.artbite.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@PostMapping("/signup")
	@ResponseStatus(HttpStatus.CREATED)
	public CustomApiResponse<Long> signup(@Valid @RequestBody SignupRequestDto signupRequestDto) {
		Long userId = authService.signup(signupRequestDto);
		return CustomApiResponse.success(userId);
	}

	@PostMapping("/login")
	public CustomApiResponse<TokenDto> login(@Valid @RequestBody LoginRequestDto loginRequestDto,
		HttpServletResponse response) {
		TokenDto tokenDto = authService.login(loginRequestDto, response);
		return CustomApiResponse.success(tokenDto);
	}

	@PostMapping("/reissue")
	public CustomApiResponse<TokenDto> reissue(HttpServletRequest request, HttpServletResponse response) {
		TokenDto tokenDto = authService.reissueAccessToken(request, response);
		return CustomApiResponse.success(tokenDto);
	}

	@PostMapping("/logout")
	public CustomApiResponse<String> logout(
		@RequestHeader("Authorization") String bearerAccessToken,
		@AuthenticationPrincipal CustomUserDetails customUserDetails,
		HttpServletRequest request, HttpServletResponse response) {
		if (customUserDetails == null) {
			throw new BusinessException(ErrorCode.COMMON_UNAUTHORIZED);
		}
		String userEmail = customUserDetails.getUsername();
		String accessToken = bearerAccessToken.substring(7);
		String socialLogoutRedirectUrl = authService.logout(accessToken, userEmail, request,
			response); // Get redirect URL
		if (socialLogoutRedirectUrl != null) {
			return CustomApiResponse.success(socialLogoutRedirectUrl); // Return URL in JSON
		}
		return CustomApiResponse.success(null);
	}
}
