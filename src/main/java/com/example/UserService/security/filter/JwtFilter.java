package com.example.UserService.security.filter;

import com.example.UserService.exception.InvalidTokenException;
import com.example.UserService.security.model.AuthUser;
import com.example.UserService.security.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            try {
                validateAccessToken(token);
                authenticate(token, request);
            } catch (InvalidTokenException ex) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("text/plain; charset=UTF-8");
                response.getWriter().write(ex.getMessage());
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void validateAccessToken(String token) {
        if (!jwtService.validateToken(token) ||
                !"access".equals(jwtService.extractTokenType(token))) {
            throw new InvalidTokenException("Invalid token");
        }
    }

    private void authenticate(String token, HttpServletRequest request) {
        Long userId = jwtService.extractUserId(token);
        String role = "ROLE_" + jwtService.extractRole(token);

        AuthUser authUser = new AuthUser(userId, role);

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        authUser,
                        token,
                        Collections.singleton(new SimpleGrantedAuthority(role))
                );


        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}