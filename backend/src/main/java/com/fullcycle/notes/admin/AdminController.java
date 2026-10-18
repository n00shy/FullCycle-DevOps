package com.fullcycle.notes.admin;
import com.fullcycle.notes.audit.*;
import com.fullcycle.notes.user.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
@RestController @RequestMapping("/api/admin")
public class AdminController {
 private final UserRepository users; private final AuditEventRepository events; private final AuditService audit; private final PasswordEncoder encoder;
 public AdminController(UserRepository users,AuditEventRepository events,AuditService audit,PasswordEncoder encoder){this.users=users;this.events=events;this.audit=audit;this.encoder=encoder;}
 @GetMapping("/users") public Page<UserView> users(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){
  if(page<0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"page must be >= 0");
  return users.findAll(PageRequest.of(page,Math.min(Math.max(size,1),100),Sort.by("id").ascending())).map(UserView::from);
 }
 @PostMapping("/users") @Transactional public UserView create(@Valid @RequestBody CreateUser body,org.springframework.security.core.Authentication auth){
  if(users.existsByUsernameIgnoreCase(body.username()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Username already exists");
  if(users.existsByEmailIgnoreCase(body.email()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Email already exists");
  AppUser actor=actor(auth);AppUser created=users.save(new AppUser(body.username().trim(),body.email().trim().toLowerCase(),encoder.encode(body.password()),body.role()));
  audit.record(actor,"USER_CREATED","USER",created.getId(),"role="+created.getRole());return UserView.from(created);
 }
 @PatchMapping("/users/{id}") @Transactional public UserView update(@PathVariable Long id,@Valid @RequestBody UpdateUser body,org.springframework.security.core.Authentication auth){
  AppUser target=users.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"User not found"));
  AppUser actor=actor(auth);
  if(body.enabled()!=null&&!body.enabled()&&target.isEnabled()&&target.getRole()==Role.ADMIN&&users.countByRoleAndEnabledTrue(Role.ADMIN)<=1)
   throw new ResponseStatusException(HttpStatus.CONFLICT,"Cannot disable the last enabled administrator");
  if(body.role()!=null&&body.role()!=Role.ADMIN&&target.isEnabled()&&target.getRole()==Role.ADMIN&&users.countByRoleAndEnabledTrue(Role.ADMIN)<=1)
   throw new ResponseStatusException(HttpStatus.CONFLICT,"Cannot demote the last enabled administrator");
  if(body.enabled()!=null)target.setEnabled(body.enabled());if(body.role()!=null)target.setRole(body.role());
  audit.record(actor,"USER_UPDATED","USER",target.getId(),"enabled="+target.isEnabled()+",role="+target.getRole());return UserView.from(target);
 }
 @GetMapping("/audit-events") public Page<AuditView> auditEvents(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="50") int size){
  if(page<0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"page must be >= 0");
  return events.findAllByOrderByOccurredAtDescIdDesc(PageRequest.of(page,Math.min(Math.max(size,1),100))).map(AuditView::from);
 }
 private AppUser actor(org.springframework.security.core.Authentication auth){return users.findById(((AppUserDetails)auth.getPrincipal()).id()).orElseThrow();}
 public record CreateUser(@NotBlank @Size(min=3,max=50) String username,@NotBlank @Email String email,@Size(min=12,max=72) String password,@NotNull Role role){}
 public record UpdateUser(Boolean enabled,Role role){}
 public record UserView(Long id,String username,String email,String role,boolean enabled,Instant createdAt){
  static UserView from(AppUser u){return new UserView(u.getId(),u.getUsername(),u.getEmail(),u.getRole().name(),u.isEnabled(),u.getCreatedAt());}
 }
 public record AuditView(Long id,Long actorUserId,String actorUsername,String action,String entityType,String entityId,Instant occurredAt,String details){
  static AuditView from(AuditEvent e){return new AuditView(e.getId(),e.getActor()==null?null:e.getActor().getId(),e.getActor()==null?null:e.getActor().getUsername(),e.getAction(),e.getEntityType(),e.getEntityId(),e.getOccurredAt(),e.getDetails());}
 }
}