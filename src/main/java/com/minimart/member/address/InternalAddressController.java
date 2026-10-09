package com.minimart.member.address;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.minimart.member.api.AddressSnapshot;
import com.minimart.member.api.MemberApi;

@RestController
@RequestMapping(MemberApi.INTERNAL_ADDRESSES)
public class InternalAddressController {

	private final AddressService addresses;

	public InternalAddressController(AddressService addresses) {
		this.addresses = addresses;
	}

	@GetMapping("/{addressId}")
	public AddressSnapshot getAddress(@PathVariable("addressId") long addressId, @RequestParam("userId") long userId) {
		return addresses.copyForOrder(addressId, userId);
	}
}
