package com.showtime.controller;

import com.showtime.dto.*;
import com.showtime.dto.request.ChangePasswordRequest;
import com.showtime.dto.request.ProfileUpdateRequest;
import com.showtime.model.User;
import com.showtime.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    @GetMapping("/profile")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('THEATER_OWNER')")
    public ResponseEntity<UserDTO> getProfile(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userService.getCurrentUser(user));
    }
    
    @PutMapping("/profile")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<UserDTO> updateProfile(
            @RequestBody ProfileUpdateRequest request,
            @AuthenticationPrincipal  User user) {
        return ResponseEntity.ok(userService.updateProfile(user, request));
    }
    
    @PostMapping("/change-password")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('THEATER_OWNER')")
    public ResponseEntity<?> changePassword(
            @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal User user) {
        userService.changePassword(user, request);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }
    
    @GetMapping("/wishlist")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<MovieDTO>> getWishlist(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userService.getWishlist(user));
    }
    
    @PostMapping("/wishlist/{movieId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> addToWishlist(
            @PathVariable Long movieId,
            @AuthenticationPrincipal User user) {
        userService.addToWishlist(user, movieId);
        return ResponseEntity.ok(Map.of("message", "Added to wishlist"));
    }
    
    @DeleteMapping("/wishlist/{movieId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> removeFromWishlist(
            @PathVariable Long movieId,
            @AuthenticationPrincipal User user) {
        userService.removeFromWishlist(user, movieId);
        return ResponseEntity.ok(Map.of("message", "Removed from wishlist"));
    }
    
    @GetMapping("/bookings")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('THEATER_OWNER')")
    public ResponseEntity<List<BookingDTO>> getUserBookings(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userService.getUserBookings(user));
    }
    
    @DeleteMapping("/profile")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('THEATER_OWNER')")
    public ResponseEntity<?> deleteProfile(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> request) {
        String password = request.get("password");
        if (password == null || password.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Password is required"));
        }
        userService.deleteUser(user, password);
        return ResponseEntity.ok(Map.of("message", "Account deleted successfully"));
    }
}