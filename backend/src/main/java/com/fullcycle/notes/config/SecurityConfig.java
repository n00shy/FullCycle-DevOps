package com.fullcycle.notes.config;
import com.fullcycle.notes.user.DatabaseUserDetailsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.*;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.*;
import java.util.List;
@Configuration
@EnableMethodSecurity
public class SecurityConfig {
 @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
 @Bean DaoAuthenticationProvider authenticationProvider(DatabaseUserDetailsService users,PasswordEncoder encoder){
  DaoAuthenticationProvider provider=new DaoAuthenticationProvider(users); provider.setPasswordEncoder(encoder); return provider;
 }
 @Bean AuthenticationManager authenticationManager(AuthenticationConfiguration config)throws Exception{return config.getAuthenticationManager();}
 @Bean SecurityContextRepository securityContextRepository(){return new HttpSessionSecurityContextRepository();}
 @Bean SessionAuthenticationStrategy sessionAuthenticationStrategy(){return new ChangeSessionIdAuthenticationStrategy();}
 @Bean CookieCsrfTokenRepository csrfTokenRepository(){CookieCsrfTokenRepository r=CookieCsrfTokenRepository.withHttpOnlyFalse();r.setCookiePath("/");return r;}
 @Bean CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origin}") String origin){
  CorsConfiguration c=new CorsConfiguration();c.setAllowedOrigins(List.of(origin));c.setAllowedMethods(List.of("GET","POST","PATCH","DELETE","OPTIONS"));
  c.setAllowedHeaders(List.of("Content-Type","X-XSRF-TOKEN","X-CSRF-TOKEN"));c.setAllowCredentials(true);
  UrlBasedCorsConfigurationSource s=new UrlBasedCorsConfigurationSource();s.registerCorsConfiguration("/api/**",c);return s;
 }
 @Bean SecurityFilterChain securityFilterChain(HttpSecurity http,DaoAuthenticationProvider provider,
   SecurityContextRepository contexts,CookieCsrfTokenRepository csrf)throws Exception{
  http.authenticationProvider(provider).cors(Customizer.withDefaults()).csrf(c->c.csrfTokenRepository(csrf))
   .securityContext(c->c.securityContextRepository(contexts))
   .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
   .authorizeHttpRequests(a->a.requestMatchers("/api/auth/csrf","/api/auth/login","/actuator/health","/actuator/health/**").permitAll()
    .requestMatchers("/api/admin/**").hasRole("ADMIN").requestMatchers(org.springframework.http.HttpMethod.OPTIONS,"/**").permitAll()
    .requestMatchers("/api/**").authenticated().requestMatchers("/actuator/**").permitAll().anyRequest().denyAll())
   .exceptionHandling(e->e.authenticationEntryPoint((req,res,ex)->res.sendError(401))
    .accessDeniedHandler((req,res,ex)->res.sendError(403)));
  return http.build();
 }
}