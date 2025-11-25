package com.okebari.artbite.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "로그인 요청 DTO")
public class LoginRequestDto {

	@NotBlank(message = "이메일은 필수입니다.")
	@Email(message = "이메일 형식이 올바르지 않습니다.")
	@Schema(description = "이메일 주소", example = "user@example.com")
	private String email;

	@NotBlank(message = "비밀번호는 필수입니다.")
	@Schema(description = "비밀번호", example = "password1234")
	private String password;
}
