package com.okebari.artbite.domain.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.okebari.artbite.domain.common.BaseTimeEntity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
public class User extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 50)
	private String email;

	@Column(nullable = false)
	private String password;

	@Column(nullable = false, length = 30)
	private String username;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private UserRole role;

	// 계정 상태를 나타내는 필드 (UserDetails에서 분리)
	@Column(nullable = false)
	private boolean enabled;
	@Column(nullable = false)
	private boolean accountNonExpired;
	@Column(nullable = false)
	private boolean accountNonLocked;
	@Column(nullable = false)
	private boolean credentialsNonExpired;

	@Column(nullable = false)
	private int tokenVersion = 0; // 토큰 무효화를 위한 버전 관리 필드

	@Builder
	public User(String email, String password, String username, UserRole role, // Existing fields
		boolean enabled, boolean accountNonExpired, boolean accountNonLocked,
		boolean credentialsNonExpired, int tokenVersion) { // New fields
		this.email = email;
		this.password = password;
		this.username = username;
		this.role = role;
		this.enabled = enabled;
		this.accountNonExpired = accountNonExpired;
		this.accountNonLocked = accountNonLocked;
		this.credentialsNonExpired = credentialsNonExpired;
		this.tokenVersion = tokenVersion;
	}

	// 토큰 버전을 증가시켜 기존 토큰을 무효화하는 메서드
	public void incrementTokenVersion() {
		this.tokenVersion++;
	}

	// UserDetails 구현이 CustomUserDetails로 분리됨
}
