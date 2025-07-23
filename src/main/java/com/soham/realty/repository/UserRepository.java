package com.soham.realty.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.soham.realty.entity.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
 Optional<User> findByEmail(String email);
 Optional<User> findByUsername(String username);
 Boolean existsByEmail(String email);
 Boolean existsByUsername(String username);
}

