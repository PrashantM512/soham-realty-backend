package com.soham.realty.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.soham.realty.entity.User;
import com.soham.realty.repository.UserRepository;
import com.soham.realty.security.UserPrincipal;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

 private final UserRepository userRepository;

 @Override
 @Transactional
 public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
     // Try to find user by email first, then by username
     User user = userRepository.findByEmail(username)
             .orElseGet(() -> userRepository.findByUsername(username)
                     .orElseThrow(() -> new UsernameNotFoundException("User not found with username or email: " + username)));

     return UserPrincipal.create(user);
 }

 @Transactional
 public UserDetails loadUserById(Long id) {
     User user = userRepository.findById(id)
             .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));

     return UserPrincipal.create(user);
 }
}

