package com.img.envops.framework.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.img.envops.common.response.R;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                 JwtTokenService jwtTokenService,
                                                 UserDetailsService userDetailsService,
                                                 ObjectMapper objectMapper,
                                                 EnvOpsApiAuthorizationManager envOpsApiAuthorizationManager) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/auth/sendCode").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/auth/codeLogin").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/routes/getConstantRoutes").permitAll()
            .requestMatchers("/error").permitAll()
            .requestMatchers("/api/**").access(envOpsApiAuthorizationManager)
            .anyRequest().permitAll())
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint((request, response, exception) ->
                writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, R.fail("401", "Unauthorized"), objectMapper))
            .accessDeniedHandler((request, response, exception) ->
                writeJson(response, HttpServletResponse.SC_FORBIDDEN, R.fail("403", "Forbidden"), objectMapper)))
        .addFilterBefore(new OncePerRequestFilter() {
          @Override
          protected void doFilterInternal(HttpServletRequest request,
                                          HttpServletResponse response,
                                          FilterChain filterChain) throws ServletException, IOException {
            String token = jwtTokenService.resolveAccessToken(request);
            if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
              String username = jwtTokenService.extractUsernameFromAccessToken(token);
              try {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    token,
                    userDetails.getAuthorities());
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
              } catch (UsernameNotFoundException exception) {
                SecurityContextHolder.clearContext();
              }
            }
            filterChain.doFilter(request, response);
          }
        }, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  private void writeJson(HttpServletResponse response,
                         int status,
                         Object body,
                         ObjectMapper objectMapper) throws IOException {
    response.setStatus(status);
    response.setCharacterEncoding("UTF-8");
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.getWriter().write(objectMapper.writeValueAsString(body));
  }
}
