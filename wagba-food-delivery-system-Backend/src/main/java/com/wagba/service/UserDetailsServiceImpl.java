package com.wagba.service;

import com.wagba.entity.User;
import com.wagba.repository.UserRepository;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
	
	private final UserRepository userRepository;
	
	public UserDetailsServiceImpl(UserRepository userRepository)
	{
		this.userRepository = userRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		User user = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found"));

		if (user.getStatus() == com.wagba.entity.enums.UserStatus.SUSPENDED
				|| user.getStatus() == com.wagba.entity.enums.UserStatus.REJECTED) {
			throw new DisabledException("This account has been suspended. Please contact support.");
		}

		// A freshly registered user has no role until the email is verified.
		// Without this guard getRole().name() below throws an NPE (HTTP 500).
		if (user.getRole() == null) {
			throw new DisabledException(user.isEmailVerified()
					? "Finish choosing your account type before logging in."
					: "Please verify your email address before logging in.");
		}

		GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().name());

		return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                List.of(authority)
        );
	}

}
