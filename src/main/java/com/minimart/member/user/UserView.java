package com.minimart.member.user;

import java.time.Instant;

public record UserView(long id, String loginName, Instant createdAt) {
}
