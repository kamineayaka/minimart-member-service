package com.minimart.member.user;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {

	private static final RowMapper<UserRow> USER = (rs, rowNum) -> new UserRow(rs.getLong("id"), rs.getString("login_name"),
			rs.getString("password_hash"), readInstant(rs, "created_at"));

	private final JdbcClient jdbc;

	public UserRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public Optional<UserRow> findById(long id) {
		return jdbc.sql("SELECT id, login_name, password_hash, created_at FROM users WHERE id = :id")
				.param("id", id)
				.query(USER)
				.optional();
	}

	public Optional<UserRow> findByLoginName(String loginName) {
		return jdbc.sql("SELECT id, login_name, password_hash, created_at FROM users WHERE login_name = :loginName")
				.param("loginName", loginName)
				.query(USER)
				.optional();
	}

	public long insert(String loginName, String passwordHash, Instant createdAt) {
		GeneratedKeyHolder keys = new GeneratedKeyHolder();
		jdbc.sql("""
				INSERT INTO users (login_name, password_hash, created_at)
				VALUES (:loginName, :passwordHash, :createdAt)
				""")
				.param("loginName", loginName)
				.param("passwordHash", passwordHash)
				.param("createdAt", Timestamp.from(createdAt))
				.update(keys);
		return GeneratedIds.from(keys);
	}

	static Instant readInstant(ResultSet rs, String column) throws SQLException {
		Timestamp timestamp = rs.getTimestamp(column);
		return timestamp.toInstant();
	}
}
