package com.dcm.backend.demo.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .cors(cors -> {}) // Enables CORS support
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // Public GET access for downloading files
                        .requestMatchers(HttpMethod.GET, "/files/**").permitAll()

                        // Public endpoints — no token required
                        .requestMatchers(
                                "/ping",
                                "/user/register",
                                "/user/login",
                                "/files/**",
                                "/register",
                                "/workers",
                                "/workers/**",
                                "/workers/heartbeat",
                                "/jobs/poll/**",
                                "/jobs/result",
                                "/jobs/fail",
                                "/jobs/artifact",
                                "/workers/withdraw",
                                "/workers/*/withdrawals",
                                "/workers/rate",
                                "/jobs/timeout"
                        ).permitAll()

                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // Everything else requires JWT
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


}