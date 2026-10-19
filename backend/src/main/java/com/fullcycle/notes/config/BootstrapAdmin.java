package com.fullcycle.notes.config;
import com.fullcycle.notes.user.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
@Configuration
public class BootstrapAdmin {
 @Bean CommandLineRunner createInitialAdmin(UserRepository users,PasswordEncoder encoder,
  @Value("${app.bootstrap-admin.username:}") String username,
  @Value("${app.bootstrap-admin.email:}") String email,
  @Value("${app.bootstrap-admin.password:}") String password){
  return args->{
   if(username.isBlank()&&email.isBlank()&&password.isBlank())return;
   if(username.isBlank()||email.isBlank()||password.isBlank())throw new IllegalStateException("Set all APP_BOOTSTRAP_ADMIN_* variables or leave all empty.");
   if(password.length()<12||password.length()>72)throw new IllegalStateException("Bootstrap admin password must be 12-72 characters.");
   if(users.count()!=0)return;
   if(users.existsByUsernameIgnoreCase(username)||users.existsByEmailIgnoreCase(email))throw new IllegalStateException("Bootstrap admin username or email already exists.");
   users.save(new AppUser(username.trim(),email.trim().toLowerCase(),encoder.encode(password),Role.ADMIN));
  };
 }
}