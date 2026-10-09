package com.minimart.member.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import com.minimart.api.error.ApiError;
import com.minimart.api.error.ApiFieldError;
import com.minimart.api.error.ErrorCodes;
import com.minimart.api.http.CorrelationHeaders;
import com.minimart.member.security.JwtProperties;
import com.minimart.member.support.MemberSpringTest;
import com.minimart.member.user.IssuedToken;
import com.minimart.member.user.UserView;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class UserHttpTest extends MemberSpringTest {

	@Autowired
	TestRestTemplate rest;

	@Autowired
	JdbcClient jdbc;

	@Autowired
	JwtEncoder jwtEncoder;

	@Autowired
	JwtProperties jwtProperties;

	@Test
	void registerCreatesUserWithoutReturningPassword() {
		String loginName = loginName();
		ResponseEntity<String> raw = rest.postForEntity("/users",
				Map.of("loginName", loginName, "password", "correct-horse"), String.class);

		assertThat(raw.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(raw.getHeaders().getContentType()).isNotNull();
		assertThat(raw.getHeaders().getContentType().toString()).contains("application/json");
		assertThat(raw.getBody()).contains("\"loginName\":\"" + loginName + "\"");
		assertThat(raw.getBody()).doesNotContain("correct-horse");
		assertThat(raw.getBody()).doesNotContain("password");

		UserView user = rest.postForEntity("/users", Map.of("loginName", loginName(), "password", "correct-horse"), UserView.class)
				.getBody();
		assertThat(user).isNotNull();
		assertThat(user.id()).isPositive();
		assertThat(user.createdAt()).isCloseTo(Instant.now(), within(2, ChronoUnit.MINUTES));
	}

	@Test
	void registerNormalizesLoginNameAndRejectsDuplicatesIgnoringCase() {
		String loginName = loginName();
		ResponseEntity<UserView> created = rest.postForEntity("/users",
				Map.of("loginName", "  " + loginName.toUpperCase() + "  ", "password", "correct-horse"), UserView.class);

		assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(created.getBody()).isNotNull();
		assertThat(created.getBody().loginName()).isEqualTo(loginName);
		assertThat(created.getHeaders().getLocation()).isNotNull();
		assertThat(created.getHeaders().getLocation().toString()).isEqualTo("/users/" + created.getBody().id());

		ResponseEntity<ApiError> duplicate = rest.postForEntity("/users",
				Map.of("loginName", loginName.toUpperCase(), "password", "correct-horse"), ApiError.class);
		assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(duplicate.getBody()).isNotNull();
		assertThat(duplicate.getBody().code()).isEqualTo("LOGIN_NAME_TAKEN");
		assertThat(duplicate.getBody().fields()).extracting(ApiFieldError::field).containsExactly("loginName");

		String hash = jdbc.sql("SELECT password_hash FROM users WHERE id = :id")
				.param("id", created.getBody().id())
				.query(String.class)
				.single();
		assertThat(hash).startsWith("$2");
		assertThat(hash).doesNotContain("correct-horse");
		assertThat(hash).isNotEqualTo("correct-horse");
	}

	@Test
	void registerRejectsBlankAndShortCredentials() {
		ResponseEntity<ApiError> response = rest.postForEntity("/users", Map.of("loginName", " ", "password", "short"),
				ApiError.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().code()).isEqualTo(ErrorCodes.VALIDATION_ERROR);
		assertThat(response.getBody().fields()).extracting(ApiFieldError::field).contains("loginName", "password");
	}

	@Test
	void registerRejectsUnreadableBody() {
		ResponseEntity<ApiError> response = rest.exchange(RequestEntity.post("/users")
				.header(HttpHeaders.CONTENT_TYPE, "application/json")
				.body("{"), ApiError.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().code()).isEqualTo(ErrorCodes.VALIDATION_ERROR);
	}

	@Test
	void issueTokenReturnsBearerJwtForTheUser() {
		String loginName = loginName();
		UserView user = rest.postForEntity("/users", Map.of("loginName", loginName, "password", "correct-horse"), UserView.class)
				.getBody();
		assertThat(user).isNotNull();

		ResponseEntity<IssuedToken> response = rest.postForEntity("/tokens",
				Map.of("loginName", " " + loginName.toUpperCase(), "password", "correct-horse"), IssuedToken.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getHeaders().getCacheControl()).isEqualTo(CacheControl.noStore().getHeaderValue());
		IssuedToken issued = response.getBody();
		assertThat(issued).isNotNull();
		assertThat(issued.tokenType()).isEqualTo("Bearer");
		assertThat(issued.expiresAt()).isAfter(Instant.now().plusSeconds(11 * 3600));
		assertThat(issued.expiresAt()).isBefore(Instant.now().plusSeconds(13 * 3600));
		assertThat(issued.token()).doesNotContain("correct-horse");
		String payload = new String(Base64.getUrlDecoder().decode(pad(issued.token().split("\\.")[1])));
		String header = new String(Base64.getUrlDecoder().decode(pad(issued.token().split("\\.")[0])));
		assertThat(header).contains("\"alg\":\"HS256\"");
		assertThat(payload).contains("\"sub\":\"" + user.id() + "\"");
		assertThat(payload).contains("\"iss\":\"" + jwtProperties.issuer() + "\"");

		ResponseEntity<UserView> me = rest.exchange(RequestEntity.get("/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + issued.token())
				.header(CorrelationHeaders.CORRELATION_ID, "me-1").build(), UserView.class);
		assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(me.getHeaders().get(CorrelationHeaders.CORRELATION_ID)).containsExactly("me-1");
		assertThat(me.getBody()).isNotNull();
		assertThat(me.getBody().id()).isEqualTo(user.id());
		assertThat(me.getBody().loginName()).isEqualTo(loginName);
	}

	@Test
	void wrongPasswordAndUnknownUserFailTheSameWay() {
		String loginName = loginName();
		rest.postForEntity("/users", Map.of("loginName", loginName, "password", "correct-horse"), UserView.class);

		ResponseEntity<ApiError> wrongPassword = rest.postForEntity("/tokens",
				Map.of("loginName", loginName, "password", "wrong-password"), ApiError.class);
		ResponseEntity<ApiError> unknownUser = rest.postForEntity("/tokens",
				Map.of("loginName", loginName(), "password", "correct-horse"), ApiError.class);

		assertThat(wrongPassword.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(unknownUser.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(wrongPassword.getBody()).isNotNull();
		assertThat(unknownUser.getBody()).isNotNull();
		assertThat(wrongPassword.getBody().code()).isEqualTo("AUTHENTICATION_FAILED");
		assertThat(unknownUser.getBody().code()).isEqualTo(wrongPassword.getBody().code());
		assertThat(unknownUser.getBody().message()).isEqualTo(wrongPassword.getBody().message());
		assertThat(wrongPassword.getBody().message()).doesNotContain(loginName);
	}

	@Test
	void missingAndTamperedTokensAreRejected() {
		ResponseEntity<ApiError> missing = rest.exchange(
				RequestEntity.get("/me").header(CorrelationHeaders.CORRELATION_ID, "no-token").build(), ApiError.class);
		assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(missing.getBody()).isNotNull();
		assertThat(missing.getBody().code()).isEqualTo("UNAUTHENTICATED");
		assertThat(missing.getBody().correlationId()).isEqualTo("no-token");
		assertThat(missing.getHeaders().get(CorrelationHeaders.CORRELATION_ID)).containsExactly("no-token");
		assertThat(missing.getHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE)).contains("Bearer");

		String noneHeader = Base64.getUrlEncoder().withoutPadding()
				.encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes());
		String nonePayload = Base64.getUrlEncoder().withoutPadding()
				.encodeToString("{\"iss\":\"member-service\",\"sub\":\"1\"}".getBytes());
		ResponseEntity<ApiError> noneAlg = rest.exchange(RequestEntity.get("/me")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + noneHeader + "." + nonePayload + ".").build(), ApiError.class);
		assertThat(noneAlg.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(noneAlg.getBody()).isNotNull();
		assertThat(noneAlg.getBody().code()).isEqualTo("UNAUTHENTICATED");

		IssuedToken issued = tokenForNewUser();
		String[] parts = issued.token().split("\\.");
		String tampered = parts[0] + "." + parts[1] + "." + parts[2].substring(0, parts[2].length() - 2) + "aa";
		ResponseEntity<ApiError> badSig = rest.exchange(
				RequestEntity.get("/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + tampered).build(), ApiError.class);
		assertThat(badSig.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

		String wrongIssuer = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
				JwtClaimsSet.builder().issuer("other-issuer").subject("1").issuedAt(Instant.now())
						.expiresAt(Instant.now().plusSeconds(300)).build()))
				.getTokenValue();
		ResponseEntity<ApiError> issuer = rest.exchange(
				RequestEntity.get("/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + wrongIssuer).build(), ApiError.class);
		assertThat(issuer.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(issuer.getBody()).isNotNull();
		assertThat(issuer.getBody().code()).isEqualTo("UNAUTHENTICATED");
	}

	@Test
	void schemaIsOnlyUsersAndAddresses() {
		List<String> tables = jdbc.sql("""
				SELECT table_name FROM information_schema.tables
				WHERE table_schema = DATABASE()
				""").query(String.class).list();
		assertThat(tables).containsExactlyInAnyOrder("address", "flyway_schema_history", "users");
	}

	private IssuedToken tokenForNewUser() {
		String loginName = loginName();
		assertThat(rest.postForEntity("/users", Map.of("loginName", loginName, "password", "correct-horse"), UserView.class)
				.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		ResponseEntity<IssuedToken> token = rest.postForEntity("/tokens",
				Map.of("loginName", loginName, "password", "correct-horse"), IssuedToken.class);
		assertThat(token.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(token.getBody()).isNotNull();
		return token.getBody();
	}

	static String loginName() {
		return "u" + UUID.randomUUID().toString().replace("-", "");
	}

	private static String pad(String base64Url) {
		int pad = (4 - base64Url.length() % 4) % 4;
		return base64Url + "=".repeat(pad);
	}
}
