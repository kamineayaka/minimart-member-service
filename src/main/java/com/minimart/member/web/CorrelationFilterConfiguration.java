package com.minimart.member.web;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import com.minimart.api.web.CorrelationIdFilter;

@Configuration
public class CorrelationFilterConfiguration {

	@Bean
	FilterRegistrationBean<CorrelationIdFilter> correlationIdFilterRegistration(CorrelationIdFilter filter) {
		FilterRegistrationBean<CorrelationIdFilter> registration = new FilterRegistrationBean<>(filter);
		registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
		return registration;
	}
}
