package com.minimart.member.user;

import java.util.Map;

import org.springframework.jdbc.support.KeyHolder;

public final class GeneratedIds {

	private GeneratedIds() {
	}

	public static long from(KeyHolder keys) {
		if (keys.getKeyList().isEmpty()) {
			throw new IllegalStateException("insert did not return an id");
		}
		Map<String, Object> row = keys.getKeyList().get(0);
		Object id = row.get("id");
		if (!(id instanceof Number)) {
			id = row.get("GENERATED_KEY");
		}
		if (!(id instanceof Number) && row.size() == 1) {
			id = row.values().iterator().next();
		}
		if (id instanceof Number number) {
			return number.longValue();
		}
		throw new IllegalStateException("insert did not return an id");
	}
}
