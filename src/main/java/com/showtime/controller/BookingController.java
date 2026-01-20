package com.showtime.controller;


import com.showtime.dto.*;
import com.showtime.model.User;
import com.showtime.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.showtime.dto.request.*;
import com.showtime.dto.response.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {
    
    private final BookingService bookingService;
    
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<BookingResponse> createBooking(
            @RequestBody BookingRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(bookingService.createBooking(request, user));
    }
    
    @GetMapping("/my-bookings")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<BookingDTO>> getMyBookings(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(bookingService.getUserBookings(user.getId()));
    }
    
    @GetMapping("/my-bookings/history")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<BookingHistoryDTO> getMyBookingHistory(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(bookingService.getUserBookingHistory(user.getId()));
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<BookingDTO> getBookingById(@PathVariable Long id, @AuthenticationPrincipal User user) {
        List<BookingDTO> bookings = bookingService.getUserBookings(user.getId());
        BookingDTO booking = bookings.stream()
            .filter(b -> b.getId().equals(id))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Booking not found"));
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
        // Implementation for getting tickets
        return ResponseEntity.ok(List.of());
    }
}