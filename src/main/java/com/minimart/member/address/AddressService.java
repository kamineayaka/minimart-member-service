package com.minimart.member.address;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.minimart.api.error.ErrorCodes;
import com.minimart.member.api.AddressSnapshot;
import com.minimart.member.web.MemberException;

@Service
public class AddressService {

	private final AddressRepository addresses;

	private final Clock clock;

	public AddressService(AddressRepository addresses, Clock clock) {
		this.addresses = addresses;
		this.clock = clock;
	}

	@Transactional
	public AddressSnapshot create(long userId, SaveAddressRequest request) {
		lockOwner(userId);
		long id = addresses.insert(userId, request, clock.instant());
		return addresses.findByIdAndUser(id, userId).orElseThrow(AddressService::missing);
	}

	@Transactional(readOnly = true)
	public List<AddressSnapshot> list(long userId) {
		return addresses.listByUser(userId);
	}

	@Transactional(readOnly = true)
	public AddressSnapshot get(long userId, long addressId) {
		return addresses.findByIdAndUser(addressId, userId).orElseThrow(AddressService::missing);
	}

	@Transactional
	public AddressSnapshot update(long userId, long addressId, SaveAddressRequest request) {
		lockOwner(userId);
		Instant now = clock.instant();
		if (addresses.update(userId, addressId, request, now) == 0) {
			throw missing();
		}
		return addresses.findByIdAndUser(addressId, userId).orElseThrow(AddressService::missing);
	}

	@Transactional
	public void delete(long userId, long addressId) {
		lockOwner(userId);
		if (addresses.delete(userId, addressId) == 0) {
			throw missing();
		}
	}

	/**
	 * Current address content for order-service to copy. Member does not keep a frozen snapshot;
	 * an Order holds whatever was returned at place time.
	 */
	@Transactional(readOnly = true)
	public AddressSnapshot copyForOrder(long addressId, long userId) {
		return addresses.findByIdAndUser(addressId, userId).orElseThrow(AddressService::missing);
	}

	private void lockOwner(long userId) {
		if (!addresses.lockUser(userId)) {
			throw new MemberException.Unauthenticated("UNAUTHENTICATED", "Authentication is required");
		}
	}

	private static MemberException.NotFound missing() {
		return new MemberException.NotFound(ErrorCodes.ADDRESS_NOT_FOUND, "Address not found");
	}
}
