package com.fullcycle.notes.user;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="users")
public class AppUser {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,length=50) private String username;
 @Column(nullable=false,length=254) private String email;
 @Column(name="password_hash",nullable=false,length=100) private String passwordHash;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private Role role=Role.USER;
 @Column(nullable=false) private boolean enabled=true;
 @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt=Instant.now();
 protected AppUser() {}
 public AppUser(String username,String email,String passwordHash,Role role){this.username=username;this.email=email;this.passwordHash=passwordHash;this.role=role;}
 public Long getId(){return id;} public String getUsername(){return username;} public String getEmail(){return email;}
 public String getPasswordHash(){return passwordHash;} public Role getRole(){return role;} public boolean isEnabled(){return enabled;}
 public Instant getCreatedAt(){return createdAt;} public void setEnabled(boolean value){enabled=value;} public void setRole(Role value){role=value;}
}