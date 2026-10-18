package com.fullcycle.notes.user;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
@Service
public class DatabaseUserDetailsService implements UserDetailsService {
 private final UserRepository users;
 public DatabaseUserDetailsService(UserRepository users){this.users=users;}
 @Override public UserDetails loadUserByUsername(String value) throws UsernameNotFoundException {
  return users.findByUsernameIgnoreCaseOrEmailIgnoreCase(value,value).map(AppUserDetails::from)
   .orElseThrow(()->new UsernameNotFoundException("User not found"));
}}