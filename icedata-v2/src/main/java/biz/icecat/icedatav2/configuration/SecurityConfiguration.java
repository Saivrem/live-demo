package biz.icecat.icedatav2.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@Configuration
public class SecurityConfiguration {

    public static final String ADMIN_AUTHORITY = "ICEDATA_ADMIN";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .authorizeHttpRequests(authorization -> authorization
                        .requestMatchers("/admin", "/admin/**").hasAuthority(ADMIN_AUTHORITY)
                        .requestMatchers(HttpMethod.POST, "/suppliers", "/suppliers/**").hasAuthority(ADMIN_AUTHORITY)
                        .anyRequest().permitAll())
                // Anonymous uploads remain public; administrative writes retain CSRF protection.
                .csrf(csrf -> csrf.ignoringRequestMatchers("/uploads", "/uploads/"))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.FORBIDDEN)))
                .build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        // Do not create Spring Boot's generated default user.
        return new InMemoryUserDetailsManager();
    }
}
