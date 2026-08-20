package com.wagba.repository;

import com.wagba.entity.Driver;
import com.wagba.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DriverRepository extends JpaRepository<Driver, Long> {

    Optional<Driver> findByUser(User user);

    boolean existsByUser(User user);
}