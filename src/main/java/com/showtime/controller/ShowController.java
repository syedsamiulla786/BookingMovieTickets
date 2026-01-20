package com.showtime.controller;

import com.showtime.dto.*;
import com.showtime.dto.request.*;
import com.showtime.dto.response.*;
import com.showtime.service.ShowService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shows")
@RequiredArgsConstructor
public class ShowController {
    
    private final ShowService showService;
    
    @GetMapping("/movie/{movieId}")
    public ResponseEntity<List<ShowDTO>> getShowsByMovie(
            @PathVariable Long movieId,
            @RequestParam(required = false) String city) {
        return ResponseEntity.ok(showService.getShowsByMovie(movieId, city));
    }
    
    @GetMapping("/movie/{movieId}/dates")
    public ResponseEntity<List<LocalDate>> getAvailableDates(@PathVariable Long movieId) {
        return ResponseEntity.ok(showService.getAvailableDatesForMovie(movieId));
    }
    
    @GetMapping("/theater/{theaterId}")
    public ResponseEntity<List<ShowDTO>> getShowsByTheater(
            @PathVariable Long theaterId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(showService.getShowsByTheater(theaterId, date));
    }
    
    @GetMapping("/{showId}/seats")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<SeatLayoutResponse> getSeatLayout(@PathVariable Long showId) {
        return ResponseEntity.ok(showService.getSeatLayout(showId));
    }
    
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ShowDTO> createShow(@RequestBody ShowRequest request) {
        return ResponseEntity.ok(showService.createShow(request));
    }
    
    @PostMapping("/{showId}/book-seats")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> bookSeats(
            @PathVariable Long showId,
            @RequestBody SeatSelectionRequest request) {
        showService.updateBookedSeats(showId, request.getSeatNumbers());
        return ResponseEntity.ok(Map.of("message", "Seats booked successfully"));
    }
}