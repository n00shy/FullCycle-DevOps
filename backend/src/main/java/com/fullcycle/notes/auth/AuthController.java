package com.fullcycle.notes.auth;
import com.fullcycle.notes.user.AppUserDetails;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.*;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequestMapping("/api/auth")
public class AuthController {
 private final AuthenticationManager manager; private final SecurityContextRepository contexts;
 public AuthController(AuthenticationManager manager,SecurityContextRepository contexts){this.manager=manager;this.contexts=contexts;}
 @GetMapping("/csrf") public Map<String,String> csrf(CsrfToken token){return Map.of("headerName",token.getHeaderName(),"token",token.getToken());}
 @PostMapping("/login") public ResponseEntity<?> login(@Valid @RequestBody LoginRequest body,HttpServletRequest req,HttpServletResponse res){
  try {
   Authentication auth=manager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(body.username(),body.password()));
   SecurityContext context=SecurityContextHolder.createEmptyContext();context.setAuthentication(auth);SecurityContextHolder.setContext(context);
   contexts.saveContext(context,req,res);AppUserDetails u=(AppUserDetails)auth.getPrincipal();
   return ResponseEntity.ok(new UserResponse(u.id(),u.username(),u.email(),u.role().name()));
  } catch(AuthenticationException ex){SecurityContextHolder.clearContext();return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error","Invalid username/email or password"));}
 }
 @PostMapping("/logout") public ResponseEntity<Void> logout(HttpServletRequest req,HttpServletResponse res){
  SecurityContextHolder.clearContext();contexts.saveContext(SecurityContextHolder.createEmptyContext(),req,res);
  HttpSession session=req.getSession(false);if(session!=null)session.invalidate();return ResponseEntity.noContent().build();
 }
 @GetMapping("/me") public UserResponse me(Authentication auth){AppUserDetails u=(AppUserDetails)auth.getPrincipal();return new UserResponse(u.id(),u.username(),u.email(),u.role().name());}
 public record LoginRequest(@NotBlank String username,@NotBlank String password){}
 public record UserResponse(Long id,String username,String email,String role){}
}