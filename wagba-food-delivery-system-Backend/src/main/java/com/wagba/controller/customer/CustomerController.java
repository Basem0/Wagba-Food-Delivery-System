package com.wagba.controller.customer;

import com.wagba.dto.AddressRequest;
import com.wagba.dto.order.AddressResponse;
import com.wagba.service.AddressService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customers/me")
public class CustomerController {

    private final AddressService addressService;

    public CustomerController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping("/address")
    public ResponseEntity<AddressResponse> getAddress() {
        AddressResponse addr = addressService.getMyAddress();
        if (addr == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(addr);
    }

    @PostMapping("/address")
    public ResponseEntity<AddressResponse> saveAddress(@RequestBody AddressRequest request) {
        return ResponseEntity.ok(addressService.saveMyAddress(request));
    }
}
