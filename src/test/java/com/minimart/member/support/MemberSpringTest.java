package com.minimart.member.support;

import java.time.Duration;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

public abstract class MemberSpringTest {

	static final MySQLContainer MYSQL = new MySQLContainer(DockerImageName.parse("mysql:8.4"))
			.withDatabaseName("minimart_member")
			.withUsername("minimart")
			.withPassword("minimart");

	static {
		MYSQL.withStartupTimeout(Duration.ofSeconds(120));
		MYSQL.withEnv("TZ", "UTC");
		MYSQL.start();
	}

	@DynamicPropertySource
	static void mysqlProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", MemberSpringTest::jdbcUrl);
		registry.add("spring.datasource.username", MYSQL::getUsername);
		registry.add("spring.datasource.password", MYSQL::getPassword);
	}

	private static String jdbcUrl() {
		String url = MYSQL.getJdbcUrl();
		String params = "useUnicode=true&characterEncoding=utf8&serverTimezone=UTC&connectionTimeZone=UTC";
		return url + (url.contains("?") ? "&" : "?") + params;
	}
}
