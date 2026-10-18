package com.fullcycle.notes.user;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;
public record AppUserDetails(Long id,String username,String password,String email,Role role,boolean enabled) implements UserDetails {
 public static AppUserDetails from(AppUser u){return new AppUserDetails(u.getId(),u.getUsername(),u.getPasswordHash(),u.getEmail(),u.getRole(),u.isEnabled());}
 @Override public Collection<? extends GrantedAuthority> getAuthorities(){return List.of(new SimpleGrantedAuthority("ROLE_"+role.name()));}
 @Override public String getPassword(){return password;}
 @Override public boolean isAccountNonExpired(){return true;}
 @Override public boolean isAccountNonLocked(){return true;}
 @Override public boolean isCredentialsNonExpired(){return true;}
 @Override public boolean isEnabled(){return enabled;}
}