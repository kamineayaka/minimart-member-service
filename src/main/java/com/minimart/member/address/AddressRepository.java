package com.minimart.member.address;

import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import com.minimart.member.api.AddressSnapshot;
import com.minimart.member.user.GeneratedIds;

@Repository
public class AddressRepository {

	private static final RowMapper<AddressSnapshot> ADDRESS = (rs, rowNum) -> new AddressSnapshot(rs.getLong("id"),
			rs.getLong("user_id"), rs.getString("receiver_name"), rs.getString("phone"), rs.getString("province"),
			rs.getString("city"), rs.getString("district"), rs.getString("detail"));

	private static final String COLUMNS = """
			id, user_id, receiver_name, phone, province, city, district, detail
			""";

	private final JdbcClient jdbc;

	public AddressRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public boolean lockUser(long userId) {
		return jdbc.sql("SELECT id FROM users WHERE id = :id FOR UPDATE")
				.param("id", userId)
				.query(Long.class)
				.optional()
				.isPresent();
	}

	public long insert(long userId, SaveAddressRequest request, Instant now) {
		GeneratedKeyHolder keys = new GeneratedKeyHolder();
		jdbc.sql("""
				INSERT INTO address (
				    user_id, receiver_name, phone, province, city, district, detail, created_at, updated_at)
				VALUES (
				    :userId, :receiverName, :phone, :province, :city, :district, :detail, :now, :now)
				""")
				.param("userId", userId)
				.param("receiverName", request.receiverName())
				.param("phone", request.phone())
				.param("province", request.province())
				.param("city", request.city())
				.param("district", request.district(), Types.VARCHAR)
				.param("detail", request.detail())
				.param("now", Timestamp.from(now))
				.update(keys);
		return GeneratedIds.from(keys);
	}

	public int update(long userId, long addressId, SaveAddressRequest request, Instant now) {
		return jdbc.sql("""
				UPDATE address
				SET receiver_name = :receiverName,
				    phone = :phone,
				    province = :province,
				    city = :city,
				    district = :district,
				    detail = :detail,
				    updated_at = :now
				WHERE id = :id AND user_id = :userId
				""")
				.param("receiverName", request.receiverName())
				.param("phone", request.phone())
				.param("province", request.province())
				.param("city", request.city())
				.param("district", request.district(), Types.VARCHAR)
				.param("detail", request.detail())
				.param("now", Timestamp.from(now))
				.param("id", addressId)
				.param("userId", userId)
				.update();
	}

	public int delete(long userId, long addressId) {
		return jdbc.sql("DELETE FROM address WHERE id = :id AND user_id = :userId")
				.param("id", addressId)
				.param("userId", userId)
				.update();
	}

	public Optional<AddressSnapshot> findByIdAndUser(long addressId, long userId) {
		return jdbc.sql("SELECT " + COLUMNS + " FROM address WHERE id = :id AND user_id = :userId")
				.param("id", addressId)
				.param("userId", userId)
				.query(ADDRESS)
				.optional();
	}

	public List<AddressSnapshot> listByUser(long userId) {
		return jdbc.sql("SELECT " + COLUMNS + " FROM address WHERE user_id = :userId ORDER BY id")
				.param("userId", userId)
				.query(ADDRESS)
				.list();
	}
}
