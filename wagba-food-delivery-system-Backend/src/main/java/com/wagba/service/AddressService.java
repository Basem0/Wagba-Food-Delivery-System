package com.wagba.service;

import com.wagba.dto.AddressRequest;
import com.wagba.dto.order.AddressResponse;
import com.wagba.entity.Address;
import com.wagba.entity.User;
import com.wagba.repository.AddressRepository;
import com.wagba.repository.UserRepository;
import com.wagba.security.SecurityUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    public AddressService(AddressRepository addressRepository, UserRepository userRepository) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
    }

    public AddressResponse getMyAddress() {
        User user = currentUser();
        List<Address> list = addressRepository.findByUserId(user.getId());
        if (list.isEmpty()) return null;
        return toResponse(list.get(0));
    }

    @Transactional
    public AddressResponse saveMyAddress(AddressRequest req) {
        User user = currentUser();
        List<Address> list = addressRepository.findByUserId(user.getId());
        Address address = list.isEmpty() ? new Address() : list.get(0);
        address.setCity(req.city());
        address.setStreet(req.street());
        address.setBuildingNumber(req.buildingNumber());
        address.setApartment(req.apartment());
        address.setDetails(req.details());
        address.setLatitude(req.latitude());
        address.setLongitude(req.longitude());
        address.setUser(user);
        address = addressRepository.save(address);
        return toResponse(address);
    }

    private User currentUser() {
        String email = SecurityUtil.getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private AddressResponse toResponse(Address a) {
        return new AddressResponse(a.getId(), a.getCity(), a.getStreet(), a.getBuildingNumber(),
                a.getApartment(), a.getDetails(), a.getLatitude(), a.getLongitude());
    }
}
