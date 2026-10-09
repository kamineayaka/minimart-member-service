package com.minimart.member.user;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
public class TokenController {

	private final UserService users;

	public TokenController(UserService users) {
		this.users = users;
	}

	@PostMapping("/tokens")
	public ResponseEntity<IssuedToken> issue(@Valid @RequestBody IssueTokenRequest request) {
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(users.issueToken(request));
	}
}
