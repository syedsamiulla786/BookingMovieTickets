package com.showtime.controller;

import com.showtime.dto.*;
import com.showtime.dto.request.*;
import com.showtime.dto.response.*;
import com.showtime.model.User;
import com.showtime.repository.UserRepository;
import com.showtime.service.AuthService;
import com.showtime.service.EmailService;
import com.showtime.util.JwtUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthService authService;
    private final EmailService emailService;
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        System.out.println("Login attempt for email: " + request.getEmail());
        
        try {
            System.out.println("Attempting authentication...");
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            
            System.out.println("Authentication successful!");
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtils.generateJwtToken(authentication);
            System.out.println("Token generated: " + jwt);
            
            UserDetails userDetails =
                    (UserDetails) authentication.getPrincipal();
            
            User user = userRepository
                    .findByEmail(userDetails.getUsername())
                    .orElseThrow();
            System.out.println("User found: " + user.getEmail() + ", Role: " + user.getRole());
            
            AuthResponse response = new AuthResponse(
            	    jwt,
            	    user.getId(),
            	    user.getName(),
            	    user.getEmail(),
            	    user.getPhone(),
            	    user.getRole().name()  
            	);

            
            return ResponseEntity.ok(response);
            
        } catch (BadCredentialsException e) {
            System.out.println("BadCredentialsException: " + e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("message", "Invalid email or password"));
        } catch (Exception e) {
            System.out.println("General Exception during login: " + e.getClass().getName());
            System.out.println("Exception message: " + e.getMessage());
            e.printStackTrace();  // This will show the full stack trace
            return ResponseEntity.badRequest()
                .body(Map.of("message", "Login failed: " + e.getMessage()));
        }
    }
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "Email is already registered"));
        }
        
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setRole(User.Role.ADMIN);
        
        userRepository.save(user);
        
        // Send welcome email
        emailService.sendWelcomeEmail(user.getEmail(), user.getName());
        
        return ResponseEntity.ok(Map.of("message", "User registered successfully"));
    }
    
    @PostMapping("/logout")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('THEATER_OWNER')")
    public ResponseEntity<?> logout() {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
    
    @PostMapping("/refresh-token")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('THEATER_OWNER')")
    public ResponseEntity<?> refreshToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtUtils.validateJwtToken(token)) {
                String username = jwtUtils.getUserNameFromJwtToken(token);
                User user = userRepository.findByEmail(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));
                
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                    user, null, user.getAuthorities()
                );
                String newToken = jwtUtils.generateJwtToken(authentication);
                
                AuthResponse response = new AuthResponse(
                    newToken, user.getId(), user.getName(), 
                    user.getEmail(), user.getPhone(), user.getRole().name()
                );
                
                return ResponseEntity.ok(response);
            }
        }
        return ResponseEntity.badRequest()
            .body(Map.of("message", "Invalid token"));
    }
    
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(Map.of("message", "Password reset link sent to your email"));
    }
    
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getToken(), request.getPassword());
        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
    }
}