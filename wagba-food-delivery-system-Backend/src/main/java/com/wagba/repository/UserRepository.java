package com.wagba.repository;

import com.wagba.entity.User;
import com.wagba.entity.enums.UserRole;
import com.wagba.entity.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    Optional<User> findByVerificationToken(String verificationToken);

    Optional<User> findByPasswordResetToken(String passwordResetToken);

    Optional<User> findByEmail(String email);

    List<User> findByRole(UserRole role);

    List<User> findByRoleAndStatus(UserRole role, UserStatus status);

    Page<User> findByRole(UserRole role, Pageable pageable);

    Page<User> findByRoleAndStatus(UserRole role, UserStatus status, Pageable pageable);

    Page<User> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String name, String email, Pageable pageable);

    /** Role-scoped search, so the drivers tab does not return customers. */
    @Query("""
            SELECT u FROM User u
            WHERE u.role = :role
              AND (LOWER(u.name) LIKE %:q% OR LOWER(u.email) LIKE %:q%)
            """)
    Page<User> searchByRole(@Param("role") UserRole role, @Param("q") String q, Pageable pageable);

    long countByRole(UserRole role);

}
