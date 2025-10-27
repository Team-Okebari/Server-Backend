package com.okebari.artbite.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.okebari.artbite.domain.user.User;
import com.okebari.artbite.domain.user.UserRole;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequestDto {

	@NotBlank(message = "이메일은 필수입니다.")
	@Email(message = "이메일 형식이 올바르지 않습니다.")
	private String email;

	@NotBlank(message = "비밀번호는 필수입니다.")
	@Pattern(regexp = ".{8,}",
		message = "비밀번호는 8자 이상이어야 합니다.")
	private String password;

	@NotBlank(message = "사용자 이름은 필수입니다.")
	private String username;

	public User toEntity(PasswordEncoder passwordEncoder) {
		return User.builder()
			.email(email)
			.password(passwordEncoder.encode(password))
			.username(username)
			.role(UserRole.USER) // 기본 역할은 USER
			.enabled(true)
			.accountNonExpired(true)
			.accountNonLocked(true)
			.credentialsNonExpired(true)
			.build();
	}
}
