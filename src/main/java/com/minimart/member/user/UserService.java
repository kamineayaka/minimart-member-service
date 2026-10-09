package com.minimart.member.user;

import java.time.Clock;
import java.time.Instant;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.minimart.member.web.MemberException;

@Service
public class UserService {

	private final UserRepository users;

	private final PasswordEncoder passwordEncoder;

	private final TokenIssuer tokens;

	private final Clock clock;

	private final String dummyHash;

	public UserService(UserRepository users, PasswordEncoder passwordEncoder, TokenIssuer tokens, Clock clock) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.tokens = tokens;
		this.clock = clock;
		this.dummyHash = passwordEncoder.encode("dummy-password-not-used-for-login");
	}

	@Transactional
	public UserView register(RegisterUserRequest request) {
		if (users.findByLoginName(request.loginName()).isPresent()) {
			throw taken();
		}
		Instant createdAt = clock.instant();
		long id;
		try {
			id = users.insert(request.loginName(), passwordEncoder.encode(request.password()), createdAt);
		}
		catch (DataIntegrityViolationException ex) {
			throw new MemberException.Conflict("LOGIN_NAME_TAKEN", "Login name is already registered", "loginName", ex);
		}
		return users.findById(id).orElseThrow().toView();
	}

	@Transactional(readOnly = true)
	public IssuedToken issueToken(IssueTokenRequest request) {
		var user = users.findByLoginName(request.loginName());
		String hash = user.map(UserRow::passwordHash).orElse(dummyHash);
		boolean passwordMatches = passwordEncoder.matches(request.password(), hash);
		if (user.isEmpty() || !passwordMatches) {
			throw new MemberException.Unauthenticated("AUTHENTICATION_FAILED", "Login name or password is wrong");
		}
		return tokens.issue(user.get().id());
	}

	@Transactional(readOnly = true)
	public UserView current(long userId) {
		return users.findById(userId)
				.map(UserRow::toView)
				.orElseThrow(() -> new MemberException.Unauthenticated("UNAUTHENTICATED", "Authentication is required"));
	}

	private static MemberException.Conflict taken() {
		return new MemberException.Conflict("LOGIN_NAME_TAKEN", "Login name is already registered", "loginName", null);
	}
}
