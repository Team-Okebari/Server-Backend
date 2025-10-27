package com.okebari.artbite.auth.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.okebari.artbite.AbstractContainerBaseTest;
import com.okebari.artbite.auth.dto.LoginRequestDto;
import com.okebari.artbite.auth.dto.SignupRequestDto;
import com.okebari.artbite.auth.dto.TokenDto;
import com.okebari.artbite.auth.service.AuthService;
import com.okebari.artbite.common.exception.EmailAlreadyExistsException;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest extends AbstractContainerBaseTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private AuthService authService;

	@Test
	@DisplayName("회원가입 성공")
	void signup_success() throws Exception {
		// given
		SignupRequestDto requestDto = new SignupRequestDto();
		requestDto.setEmail("test@example.com");
		requestDto.setPassword("password");
		requestDto.setUsername("testuser");

		when(authService.signup(any(SignupRequestDto.class))).thenReturn(1L);

		// when & then
		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestDto)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data").value(1L));
	}

	@Test
	@DisplayName("회원가입 실패 - 이메일 중복")
	void signup_fail_duplicateEmail() throws Exception {
		// given
		SignupRequestDto requestDto = new SignupRequestDto();
		requestDto.setEmail("test@example.com");
		requestDto.setPassword("password");
		requestDto.setUsername("testuser");

		when(authService.signup(any(SignupRequestDto.class))).thenThrow(new EmailAlreadyExistsException());

		// when & then
		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestDto)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("A001"));
	}

	@Test
	@DisplayName("로그인 성공")
	void login_success() throws Exception {
		// given
		LoginRequestDto requestDto = new LoginRequestDto();
		requestDto.setEmail("test@example.com");
		requestDto.setPassword("password");

		TokenDto tokenDto = TokenDto.builder().accessToken("accessToken").build();
		when(authService.login(any(LoginRequestDto.class), any())).thenReturn(tokenDto);

		// when & then
		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestDto)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.accessToken").value("accessToken"));
	}

	@Test
	@DisplayName("토큰 재발급 성공")
	void reissue_success() throws Exception {
		// given
		TokenDto tokenDto = TokenDto.builder().accessToken("newAccessToken").build();
		when(authService.reissueAccessToken(any(), any())).thenReturn(tokenDto);

		// when & then
		mockMvc.perform(post("/api/auth/reissue")
				.cookie(new Cookie("refreshToken", "refreshToken")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.accessToken").value("newAccessToken"));
	}

	@Test
	@DisplayName("로그아웃 성공")
	@WithMockUser
	void logout_success() throws Exception {
		// given
		doNothing().when(authService).logout(any(), any(), any(), any());

		// when & then
		mockMvc.perform(post("/api/auth/logout")
				.header("Authorization", "Bearer accessToken"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true));
	}
}
