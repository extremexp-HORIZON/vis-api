package gr.imsi.athenarc.xtremexpvisapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Disable CSRF protection
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/api/**").permitAll() // Allow all requests to /api**/
                .requestMatchers("/experiments/**").permitAll() // Allow all requests to /api**/
                .requestMatchers("/error").permitAll());
        return http.build();
    }
}