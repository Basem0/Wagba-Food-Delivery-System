package com.wagba.repository;

import com.wagba.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);
    
    Optional<User> findByVerificationToken(String verificationToken);

}
