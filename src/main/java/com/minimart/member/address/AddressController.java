package com.minimart.member.address;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.minimart.member.api.AddressSnapshot;
import com.minimart.member.security.AuthenticatedUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/addresses")
public class AddressController {

	private final AddressService addresses;

	private final AuthenticatedUser currentUser;

	public AddressController(AddressService addresses, AuthenticatedUser currentUser) {
		this.addresses = addresses;
		this.currentUser = currentUser;
	}

	@GetMapping
	public List<AddressSnapshot> list() {
		return addresses.list(currentUser.id());
	}

	@PostMapping
	public ResponseEntity<AddressSnapshot> create(@Valid @RequestBody SaveAddressRequest request) {
		AddressSnapshot saved = addresses.create(currentUser.id(), request);
		return ResponseEntity.created(URI.create("/addresses/" + saved.addressId())).body(saved);
	}

	@GetMapping("/{addressId}")
	public AddressSnapshot get(@PathVariable("addressId") long addressId) {
		return addresses.get(currentUser.id(), addressId);
	}

	@PutMapping("/{addressId}")
	public AddressSnapshot update(@PathVariable("addressId") long addressId, @Valid @RequestBody SaveAddressRequest request) {
		return addresses.update(currentUser.id(), addressId, request);
	}

	@DeleteMapping("/{addressId}")
	public ResponseEntity<Void> delete(@PathVariable("addressId") long addressId) {
		addresses.delete(currentUser.id(), addressId);
		return ResponseEntity.noContent().build();
	}
}
