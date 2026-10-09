package com.minimart.member.user;

import java.time.Instant;

record UserRow(long id, String loginName, String passwordHash, Instant createdAt) {

	UserView toView() {
		return new UserView(id, loginName, createdAt);
	}

	@Override
	public String toString() {
		return "UserRow[id=" + id + ", loginName=" + loginName + "]";
	}
}
