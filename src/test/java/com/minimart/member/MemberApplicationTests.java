package com.minimart.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import com.minimart.api.http.CorrelationHeaders;
import com.minimart.member.support.MemberSpringTest;

@SpringBootTest
@AutoConfigureMockMvc
class MemberApplicationTests extends MemberSpringTest {

	@Autowired
	MockMvc mockMvc;

	@Test
	void doesNotEnableFeignClients() {
		assertThat(MemberApplication.class.getAnnotations())
				.noneMatch(annotation -> annotation.annotationType().getName().contains("EnableFeignClients"));
	}

	@Test
	void healthPropagatesCorrelationId() throws Exception {
		mockMvc.perform(get("/actuator/health").header(CorrelationHeaders.CORRELATION_ID, "member-corr"))
				.andExpect(status().isOk())
				.andExpect(header().string(CorrelationHeaders.CORRELATION_ID, "member-corr"));
	}

	@Test
	void applicationYamlIsSelfContainedWithoutNacos() throws Exception {
		String yaml = Files.readString(Path.of("src/main/resources/application.yaml"));
		assertThat(yaml).doesNotContain("nacos");
		assertThat(yaml).doesNotContain("product-service");
		assertThat(yaml).doesNotContain("order-service");
		assertThat(yaml).doesNotContain("payment-service");
		assertThat(yaml).doesNotContain("feign");
		assertThat(Path.of("src/main/resources/application-runtime.yaml")).doesNotExist();
	}
}
