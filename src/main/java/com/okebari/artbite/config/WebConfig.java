package com.okebari.artbite.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(paymentPageableResolver());
		resolvers.add(accessLogPageableResolver());
	}

	@Bean
	@Qualifier("paymentPageable")
	public PageableHandlerMethodArgumentResolver paymentPageableResolver() {
		PageableHandlerMethodArgumentResolver resolver = new PageableHandlerMethodArgumentResolver();
		resolver.setPageParameterName("paymentPage");
		resolver.setSizeParameterName("paymentSize");
		resolver.setOneIndexedParameters(false);
		return resolver;
	}

	@Bean
	@Qualifier("accessLogPageable")
	public PageableHandlerMethodArgumentResolver accessLogPageableResolver() {
		PageableHandlerMethodArgumentResolver resolver = new PageableHandlerMethodArgumentResolver();
		resolver.setPageParameterName("logPage");
		resolver.setSizeParameterName("logSize");
		resolver.setOneIndexedParameters(false);
		return resolver;
	}
}
