package com.okebari.artbite.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.okebari.artbite.auth.dto.LoginRequestDto;
import com.okebari.artbite.auth.dto.SignupRequestDto;
import com.okebari.artbite.auth.dto.TokenDto;
import com.okebari.artbite.auth.jwt.JwtProvider;
import com.okebari.artbite.auth.service.AuthService;
import com.okebari.artbite.common.dto.CustomApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;
	private final JwtProvider jwtProvider;

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
		@RequestHeader(value = "Authorization", required = false) String bearerAccessToken,
		HttpServletRequest request, HttpServletResponse response) {

		String userEmail = null;

		if (bearerAccessToken != null && bearerAccessToken.startsWith("Bearer ")) {
			String accessToken = bearerAccessToken.substring(7);
			try {
				userEmail = jwtProvider.getAuthentication(accessToken).getName();
			} catch (Exception e) {
				// It's okay if we can't get the email, just log it.
				// The main goal is to invalidate the refresh token.
			}
		}

		String socialLogoutRedirectUrl = authService.logout(bearerAccessToken, userEmail, request, response);
		if (socialLogoutRedirectUrl != null) {
			return CustomApiResponse.success(socialLogoutRedirectUrl); // Return URL in JSON
		}
		return CustomApiResponse.success(null);
	}
}
