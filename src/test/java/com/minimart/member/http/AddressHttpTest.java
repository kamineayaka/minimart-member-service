package com.minimart.member.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;

import com.minimart.api.error.ApiError;
import com.minimart.api.error.ApiFieldError;
import com.minimart.api.error.ErrorCodes;
import com.minimart.api.http.CorrelationHeaders;
import com.minimart.member.api.AddressSnapshot;
import com.minimart.member.api.MemberApi;
import com.minimart.member.support.MemberSpringTest;
import com.minimart.member.user.IssuedToken;
import com.minimart.member.user.UserView;

import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class AddressHttpTest extends MemberSpringTest {

	@Autowired
	TestRestTemplate rest;

	@Autowired
	JsonMapper jsonMapper;

	@Test
	void userManagesOnlyTheirOwnAddresses() throws Exception {
		LoggedIn owner = register();
		LoggedIn other = register();

		ResponseEntity<ApiError> anonymous = rest.exchange(
				RequestEntity.get("/addresses").header(CorrelationHeaders.CORRELATION_ID, "addr-anon").build(), ApiError.class);
		assertThat(anonymous.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(anonymous.getBody()).isNotNull();
		assertThat(anonymous.getBody().code()).isEqualTo("UNAUTHENTICATED");
		assertThat(anonymous.getBody().correlationId()).isEqualTo("addr-anon");

		Map<String, Object> create = Map.of(
				"receiverName", " 张三 ",
				"phone", " 13800000000 ",
				"province", "上海",
				"city", "上海",
				"district", "  ",
				"detail", "张江路 1'号",
				"userId", 999_999L,
				"addressId", 888_888_888L);
		ResponseEntity<String> created = rest.exchange(RequestEntity.post("/addresses").headers(bearer(owner.token())).body(create),
				String.class);
		assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(created.getBody()).doesNotContain("district");
		assertThat(created.getBody()).doesNotContain("999999");
		AddressSnapshot snapshot = jsonMapper.readValue(created.getBody(), AddressSnapshot.class);
		assertThat(snapshot.userId()).isEqualTo(owner.userId());
		assertThat(snapshot.addressId()).isNotEqualTo(888_888_888L);
		assertThat(snapshot.receiverName()).isEqualTo("张三");
		assertThat(snapshot.phone()).isEqualTo("13800000000");
		assertThat(snapshot.district()).isNull();
		assertThat(snapshot.detail()).isEqualTo("张江路 1'号");
		assertThat(created.getHeaders().getLocation().toString()).isEqualTo("/addresses/" + snapshot.addressId());

		ResponseEntity<String> withDistrict = rest.exchange(RequestEntity.post("/addresses").headers(bearer(owner.token()))
				.body(address("李四", "浦东")), String.class);
		assertThat(withDistrict.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		AddressSnapshot second = jsonMapper.readValue(withDistrict.getBody(), AddressSnapshot.class);
		assertThat(second.district()).isEqualTo("浦东");

		ResponseEntity<List<AddressSnapshot>> list = rest.exchange(
				RequestEntity.get("/addresses").headers(bearer(owner.token())).build(), new ParameterizedTypeReference<>() {
				});
		assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(list.getBody()).extracting(AddressSnapshot::addressId).containsExactly(snapshot.addressId(), second.addressId());

		ResponseEntity<List<AddressSnapshot>> otherList = rest.exchange(
				RequestEntity.get("/addresses").headers(bearer(other.token())).build(), new ParameterizedTypeReference<>() {
				});
		assertThat(otherList.getBody()).isEmpty();

		ResponseEntity<ApiError> stolen = rest.exchange(
				RequestEntity.get("/addresses/" + snapshot.addressId()).headers(bearer(other.token()))
						.header(CorrelationHeaders.CORRELATION_ID, "stolen").build(),
				ApiError.class);
		assertThat(stolen.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(stolen.getBody()).isNotNull();
		assertThat(stolen.getBody().code()).isEqualTo(ErrorCodes.ADDRESS_NOT_FOUND);
		assertThat(stolen.getHeaders().get(CorrelationHeaders.CORRELATION_ID)).containsExactly("stolen");

		ResponseEntity<ApiError> stolenWrite = rest.exchange(RequestEntity.put("/addresses/" + snapshot.addressId())
				.headers(bearer(other.token())).body(address("黑客", "静安")), ApiError.class);
		assertThat(stolenWrite.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

		ResponseEntity<AddressSnapshot> updated = rest.exchange(RequestEntity.put("/addresses/" + snapshot.addressId())
				.headers(bearer(owner.token())).body(address("王五", "徐汇")), AddressSnapshot.class);
		assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(updated.getBody()).isNotNull();
		assertThat(updated.getBody().receiverName()).isEqualTo("王五");
		assertThat(updated.getBody().district()).isEqualTo("徐汇");
		assertThat(updated.getBody().addressId()).isEqualTo(snapshot.addressId());
		assertThat(updated.getBody().userId()).isEqualTo(owner.userId());

		ResponseEntity<AddressSnapshot> copied = rest.exchange(RequestEntity
				.get(MemberApi.INTERNAL_ADDRESSES + "/" + snapshot.addressId() + "?userId=" + owner.userId())
				.header(CorrelationHeaders.CORRELATION_ID, "copy-1").build(), AddressSnapshot.class);
		assertThat(copied.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(copied.getHeaders().get(CorrelationHeaders.CORRELATION_ID)).containsExactly("copy-1");
		assertThat(copied.getBody()).isEqualTo(updated.getBody());

		ResponseEntity<ApiError> wrongUser = rest.exchange(RequestEntity
				.get(MemberApi.INTERNAL_ADDRESSES + "/" + snapshot.addressId() + "?userId=" + other.userId())
				.header(CorrelationHeaders.CORRELATION_ID, "copy-miss").build(), ApiError.class);
		assertThat(wrongUser.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(wrongUser.getBody()).isNotNull();
		assertThat(wrongUser.getBody().code()).isEqualTo(ErrorCodes.ADDRESS_NOT_FOUND);
		assertThat(wrongUser.getBody().correlationId()).isEqualTo("copy-miss");

		ResponseEntity<ApiError> missingUser = rest.exchange(
				RequestEntity.get(MemberApi.INTERNAL_ADDRESSES + "/" + snapshot.addressId()).build(), ApiError.class);
		assertThat(missingUser.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(missingUser.getBody()).isNotNull();
		assertThat(missingUser.getBody().code()).isEqualTo(ErrorCodes.VALIDATION_ERROR);
		assertThat(missingUser.getBody().fields()).extracting(ApiFieldError::field).containsExactly("userId");

		ResponseEntity<ApiError> badId = rest.exchange(
				RequestEntity.get(MemberApi.INTERNAL_ADDRESSES + "/nope?userId=" + owner.userId()).build(), ApiError.class);
		assertThat(badId.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(badId.getBody().code()).isEqualTo(ErrorCodes.VALIDATION_ERROR);

		ResponseEntity<Void> deleted = rest.exchange(
				RequestEntity.delete("/addresses/" + snapshot.addressId()).headers(bearer(owner.token())).build(), Void.class);
		assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

		ResponseEntity<ApiError> gone = rest.exchange(RequestEntity
				.get(MemberApi.INTERNAL_ADDRESSES + "/" + snapshot.addressId() + "?userId=" + owner.userId()).build(),
				ApiError.class);
		assertThat(gone.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(gone.getBody().code()).isEqualTo(ErrorCodes.ADDRESS_NOT_FOUND);

		ResponseEntity<ApiError> deleteAgain = rest.exchange(
				RequestEntity.delete("/addresses/" + snapshot.addressId()).headers(bearer(owner.token())).build(), ApiError.class);
		assertThat(deleteAgain.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

		ResponseEntity<ApiError> blank = rest.exchange(
				RequestEntity.post("/addresses").headers(bearer(owner.token())).body(Map.of(
						"receiverName", " ",
						"phone", "13800000000",
						"province", "上海",
						"city", "上海",
						"detail", "张江")),
				ApiError.class);
		assertThat(blank.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(blank.getBody().code()).isEqualTo(ErrorCodes.VALIDATION_ERROR);
		assertThat(blank.getBody().fields()).extracting(ApiFieldError::field).contains("receiverName");
	}

	private LoggedIn register() {
		String loginName = UserHttpTest.loginName();
		ResponseEntity<UserView> user = rest.postForEntity("/users",
				Map.of("loginName", loginName, "password", "correct-horse"), UserView.class);
		assertThat(user.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(user.getBody()).isNotNull();
		ResponseEntity<IssuedToken> token = rest.postForEntity("/tokens",
				Map.of("loginName", loginName, "password", "correct-horse"), IssuedToken.class);
		assertThat(token.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(token.getBody()).isNotNull();
		return new LoggedIn(user.getBody().id(), token.getBody().token());
	}

	private static HttpHeaders bearer(String token) {
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(token);
		return headers;
	}

	private static Map<String, String> address(String receiver, String district) {
		return Map.of(
				"receiverName", receiver,
				"phone", "13800000000",
				"province", "上海",
				"city", "上海",
				"district", district,
				"detail", "张江路 1 号");
	}

	private record LoggedIn(long userId, String token) {
	}
}
