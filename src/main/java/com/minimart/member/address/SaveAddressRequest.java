package com.minimart.member.address;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SaveAddressRequest(
		@NotBlank @Size(max = 64) String receiverName,
		@NotBlank @Size(max = 32) String phone,
		@NotBlank @Size(max = 64) String province,
		@NotBlank @Size(max = 64) String city,
		@Size(max = 64) String district,
		@NotBlank @Size(max = 255) String detail) {

	public SaveAddressRequest {
		receiverName = trim(receiverName);
		phone = trim(phone);
		province = trim(province);
		city = trim(city);
		district = emptyToNull(trim(district));
		detail = trim(detail);
	}

	private static String trim(String value) {
		return value == null ? null : value.trim();
	}

	private static String emptyToNull(String value) {
		return value == null || value.isEmpty() ? null : value;
	}
}
