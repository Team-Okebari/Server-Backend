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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;
	private final JwtProvider jwtProvider;

	@Operation(summary = "일반 회원가입", description = "이메일, 비밀번호, 사용자 이름으로 회원가입을 진행합니다.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "201", description = "회원가입 성공", content = @Content(mediaType = "application/json",
			schema = @Schema(implementation = CustomApiResponse.class),
			examples = @ExampleObject(name = "Success", summary = "회원가입 성공",
				value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":true,\"data\":1,\"error\":null}"))),
		@ApiResponse(responseCode = "400", description = "입력값 유효성 검증 실패", content = @Content(mediaType = "application/json",
			schema = @Schema(implementation = CustomApiResponse.class),
			examples = @ExampleObject(name = "BadRequest", summary = "유효성 검증 실패",
				value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":false,\"data\":null,\"error\":{\"code\":\"C001\",\"message\":\"입력값이 유효하지 않습니다.\"}}"))),
		@ApiResponse(responseCode = "409", description = "이메일 중복", content = @Content(mediaType = "application/json",
			schema = @Schema(implementation = CustomApiResponse.class),
			examples = @ExampleObject(name = "EmailConflict", summary = "이메일 중복",
				value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":false,\"data\":null,\"error\":{\"code\":\"A001\",\"message\":\"이미 가입된 이메일입니다.\"}}")))
	})
	@PostMapping("/signup")
	@ResponseStatus(HttpStatus.CREATED)
	public CustomApiResponse<Long> signup(@Valid @RequestBody SignupRequestDto signupRequestDto) {
		Long userId = authService.signup(signupRequestDto);
		return CustomApiResponse.success(userId);
	}

	@Operation(summary = "일반 로그인", description = "이메일, 비밀번호로 로그인을 진행하고 Access Token을 발급받습니다.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "로그인 성공"),
		@ApiResponse(responseCode = "401", description = "이메일 또는 비밀번호 불일치", content = @Content(mediaType = "application/json",
			schema = @Schema(implementation = CustomApiResponse.class),
			examples = @ExampleObject(name = "InvalidCredentials", summary = "로그인 실패",
				value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":false,\"data\":null,\"error\":{\"code\":\"A002\",\"message\":\"이메일 또는 비밀번호가 일치하지 않습니다.\"}}")))
	})
	@PostMapping("/login")
	public CustomApiResponse<TokenDto> login(@Valid @RequestBody LoginRequestDto loginRequestDto,
		HttpServletResponse response) {
		TokenDto tokenDto = authService.login(loginRequestDto, response);
		return CustomApiResponse.success(tokenDto);
	}

	@Operation(summary = "Access Token 재발급", description = "유효한 Refresh Token을 사용하여 새로운 Access Token을 재발급합니다.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "토큰 재발급 성공"),
		@ApiResponse(responseCode = "401", description = "유효하지 않은 Refresh Token", content = @Content(mediaType = "application/json",
			schema = @Schema(implementation = CustomApiResponse.class),
			examples = @ExampleObject(name = "InvalidToken", summary = "유효하지 않은 토큰",
				value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":false,\"data\":null,\"error\":{\"code\":\"A004\",\"message\":\"유효하지 않은 토큰입니다.\"}}")))
	})
	@PostMapping("/reissue")
	public CustomApiResponse<TokenDto> reissue(HttpServletRequest request, HttpServletResponse response) {
		TokenDto tokenDto = authService.reissueAccessToken(request, response);
		return CustomApiResponse.success(tokenDto);
	}

	@Operation(summary = "로그아웃", description = "서버에서 Refresh Token을 만료시킵니다. 소셜 로그인의 경우 로그아웃 페이지 URL을 반환할 수 있습니다.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "로그아웃 성공", content = @Content(mediaType = "application/json",
			schema = @Schema(implementation = CustomApiResponse.class),
			examples = {
				@ExampleObject(name = "NormalLogout", summary = "일반 로그아웃 성공",
					value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":true,\"data\":null,\"error\":null}"),
				@ExampleObject(name = "SocialLogout", summary = "소셜 로그아웃 리다이렉트",
					value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":true,\"data\":\"https://social-provider.com/logout?redirect_uri=...\",\"error\":null}")
			})),
		@ApiResponse(responseCode = "401", description = "유효하지 않은 Access Token", content = @Content(mediaType = "application/json",
			schema = @Schema(implementation = CustomApiResponse.class),
			examples = @ExampleObject(name = "InvalidToken", summary = "유효하지 않은 토큰",
				value = "{\"timestamp\":\"2025-11-25T10:30:00Z\",\"success\":false,\"data\":null,\"error\":{\"code\":\"A004\",\"message\":\"유효하지 않은 토큰입니다.\"}}")))
	})
	@SecurityRequirement(name = "bearerAuth")
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
				// Ignore exceptions on logout
			}
		}

		String socialLogoutRedirectUrl = authService.logout(bearerAccessToken, userEmail, request, response);
		if (socialLogoutRedirectUrl != null) {
			return CustomApiResponse.success(socialLogoutRedirectUrl); // Return URL in JSON
		}
		return CustomApiResponse.success(null);
	}
}
