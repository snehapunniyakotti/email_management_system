package com.gmail.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;

import com.gmail.demo.entity.User;
import java.util.List;


public interface UserRepository extends JpaRepository<User, Long> {
	UserDetails findByUsername(String username);
	
	User findByEmail(String email);
}
