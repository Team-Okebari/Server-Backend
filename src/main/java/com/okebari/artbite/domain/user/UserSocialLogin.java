package com.okebari.artbite.domain.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_social_logins",
	uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "providerId"}))
public class UserSocialLogin {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false, length = 20)
	private String provider; // google, kakao, naver

	@Column(nullable = false, length = 255)
	private String providerId; // ID from the social provider

	@Builder
	public UserSocialLogin(User user, String provider, String providerId) {
		this.user = user;
		this.provider = provider;
		this.providerId = providerId;
	}
}
