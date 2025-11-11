package com.okebari.artbite.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.okebari.artbite.auth.handler.OAuth2LoginFailureHandler;
import com.okebari.artbite.auth.handler.OAuth2LoginSuccessHandler;
import com.okebari.artbite.auth.jwt.JwtAccessDeniedHandler;
import com.okebari.artbite.auth.jwt.JwtAuthenticationEntryPoint;
import com.okebari.artbite.auth.jwt.JwtAuthenticationFilter;
import com.okebari.artbite.auth.service.CustomOAuth2UserService;
import com.okebari.artbite.auth.service.CustomOidcUserService;
import com.okebari.artbite.common.filter.MdcLoggingFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final MdcLoggingFilter mdcLoggingFilter;
	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
	private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
	private final AuthenticationConfiguration authenticationConfiguration;

	// OAuth2 관련 빈 주입
	private final CustomOAuth2UserService customOAuth2UserService;
	private final CustomOidcUserService customOidcUserService;
	private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
	private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler;

	@Value("${security.whitelisted-paths}")
	private String[] whitelistedPaths;

	@Value("${cors.allowed-origins}")
	private String[] corsAllowedOrigins;

	@Bean
	public AuthenticationManager authenticationManager() throws Exception {
		return authenticationConfiguration.getAuthenticationManager();
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.csrf(AbstractHttpConfigurer::disable)
			.cors(cors -> cors.configurationSource(corsConfigurationSource())) // CORS 설정 추가
			.exceptionHandling(exceptionHandling -> exceptionHandling
				.authenticationEntryPoint(jwtAuthenticationEntryPoint)
				.accessDeniedHandler(jwtAccessDeniedHandler)
			)
			.sessionManagement(sessionManagement -> sessionManagement
				.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
			)
			.logout(AbstractHttpConfigurer::disable)
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers(whitelistedPaths).permitAll() //application-*.yml에서 화이트리스트 관리
				.anyRequest().authenticated()
			)
			.oauth2Login(oauth2 -> oauth2
				.userInfoEndpoint(userInfo -> userInfo
					.userService(customOAuth2UserService)
					.oidcUserService(customOidcUserService)
				)
				.successHandler(oAuth2LoginSuccessHandler)
				.failureHandler(oAuth2LoginFailureHandler)
			)
			.headers(headers -> headers
				.frameOptions(frameOptions -> frameOptions.sameOrigin()) // X-Frame-Options
				.httpStrictTransportSecurity(Customizer.withDefaults()) // HSTS (with default settings)
				.contentTypeOptions(Customizer.withDefaults()) // X-Content-Type-Options: nosniff
			).addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
			.addFilterBefore(mdcLoggingFilter, JwtAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();

		configuration.setAllowedOrigins(Arrays.asList(corsAllowedOrigins)); // application-*.yml에서 설정된 오리진 허용
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("*"));
		configuration.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}
