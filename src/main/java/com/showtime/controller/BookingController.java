package com.showtime.controller;


import com.showtime.dto.*;
import com.showtime.model.Booking;
import com.showtime.model.User;
import com.showtime.repository.BookingRepository;
import com.showtime.repository.UserRepository;
import com.showtime.service.BookingService;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import com.showtime.dto.request.*;
import com.showtime.dto.response.*;
import com.showtime.exception.ResourceNotFoundException;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {
//    @Autowired
    private final BookingService bookingService;
//    @Autowired
    private final BookingRepository bookingRepository;
//    @Autowired
    private final UserRepository userRepository;
    
    @PostMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<BookingResponse> createBooking(
            @RequestBody BookingRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        return ResponseEntity.ok(bookingService.createBooking(request, user));
    }
    
    @GetMapping("/my-bookings")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<BookingDTO>> getMyBookings(@AuthenticationPrincipal org.springframework.security.core.userdetails.User user) {
        return ResponseEntity.ok(bookingService.getUserBookings(user.getUsername()));
    }
    
    @GetMapping("/my-bookings/history")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<BookingHistoryDTO> getMyBookingHistory(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(bookingService.getUserBookingHistory(user.getId()));
    }
       
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getBookingById(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) throws AccessDeniedException {
        
        // Check if booking belongs to user
        Booking booking = bookingRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", id));
        
        if (!booking.getUser().getId().equals(user.getId()) && 
            !user.getRole().equals(User.Role.ADMIN)) {
            throw new AccessDeniedException("You don't have permission to view this booking");
        }
        
        return ResponseEntity.ok(booking);
    }
    
    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelBooking(
            @PathVariable Long id,
            @RequestBody CancelBookingRequest request,
            @AuthenticationPrincipal User user) {
        bookingService.cancelBooking(id, request.getReason());
        return ResponseEntity.ok(Map.of("message", "Booking cancelled successfully"));
    }
    
    @GetMapping("/{id}/tickets")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<TicketDTO>> getBookingTickets(@PathVariable Long id) {
        return ResponseEntity.ok(List.of());
    }
    
    
}