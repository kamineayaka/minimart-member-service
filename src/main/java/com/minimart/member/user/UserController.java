package com.minimart.member.user;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.minimart.member.security.AuthenticatedUser;

import jakarta.validation.Valid;

@RestController
public class UserController {

	private final UserService users;

	private final AuthenticatedUser currentUser;

	public UserController(UserService users, AuthenticatedUser currentUser) {
		this.users = users;
		this.currentUser = currentUser;
	}

	@PostMapping("/users")
	public ResponseEntity<UserView> register(@Valid @RequestBody RegisterUserRequest request) {
		UserView user = users.register(request);
		return ResponseEntity.created(URI.create("/users/" + user.id())).body(user);
	}

	@GetMapping("/me")
	public UserView me() {
		return users.current(currentUser.id());
	}
}
