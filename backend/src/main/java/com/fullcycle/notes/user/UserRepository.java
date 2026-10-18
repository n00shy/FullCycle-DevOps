package com.fullcycle.notes.user;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface UserRepository extends JpaRepository<AppUser,Long> {
 Optional<AppUser> findByUsernameIgnoreCaseOrEmailIgnoreCase(String username,String email);
 boolean existsByUsernameIgnoreCase(String username);
 boolean existsByEmailIgnoreCase(String email);
 long countByRoleAndEnabledTrue(Role role);
}