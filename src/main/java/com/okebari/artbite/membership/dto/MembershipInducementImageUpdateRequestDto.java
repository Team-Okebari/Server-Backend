package com.okebari.artbite.membership.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MembershipInducementImageUpdateRequestDto {
	@NotBlank(message = "Image URL cannot be blank")
	private String imageUrl;
}
