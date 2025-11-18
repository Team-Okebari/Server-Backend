package com.okebari.artbite.domain.membership;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "membership_inducement_image")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MembershipInducementImage extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "image_url", nullable = false, length = 500)
	private String imageUrl;

	@Builder
	public MembershipInducementImage(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public void updateImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}
}
