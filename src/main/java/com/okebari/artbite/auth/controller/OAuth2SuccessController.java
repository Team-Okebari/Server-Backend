package com.okebari.artbite.auth.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class OAuth2SuccessController {

	@GetMapping("/oauth2/success-test")
	public String oauth2SuccessTest() {
		log.info("OAuth2 success test endpoint hit.");
		return "OAuth2 로그인 성공! You can close this window.";
	}

	@GetMapping("/oauth2/logout/kakao-success")
	public String kakaoLogoutSuccess() {
		log.info("Kakao logout success endpoint hit.");
		return "Kakao 로그아웃 성공! You can close this window.";
	}
}
