package com.showtime.controller;

import com.showtime.dto.*;
import com.showtime.dto.request.SeatSelectionRequest;
import com.showtime.dto.request.ShowRequest;
import com.showtime.dto.response.SeatLayoutResponse;
import com.showtime.service.ShowService;
import jakarta.validation.Valid;
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
    
    // GET ALL SHOWS (For Admin)
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ShowDTO>> getAllShows() {
        return ResponseEntity.ok(showService.getAllShows());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ShowDTO> getShowById(@PathVariable Long id) {
        return ResponseEntity.ok(showService.getShowById(id));
    }
    
    @GetMapping("/movie/{movieId}")
    public ResponseEntity<List<ShowDTO>> getShowsByMovie(
            @PathVariable Long movieId,
            @RequestParam(required = false) String city) {
        System.out.println("Show Controller - Get Shows By movieId , City : "+movieId +"--"+city);
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
    
    // CREATE
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ShowDTO> createShow(@Valid @RequestBody ShowRequest request) {
        System.out.println("Show Creation Controller : " + request);
        return ResponseEntity.ok(showService.createShow(request));
    }
    
    // UPDATE
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ShowDTO> updateShow(
            @PathVariable Long id,
            @Valid @RequestBody ShowRequest request) {
        return ResponseEntity.ok(showService.updateShow(id, request));
    }
    
    // DELETE
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteShow(@PathVariable Long id) {
        showService.deleteShow(id);
        return ResponseEntity.ok(Map.of("message", "Show deleted successfully"));
    }
    
    // Toggle Active Status
    @PatchMapping("/{id}/toggle-active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ShowDTO> toggleShowActive(@PathVariable Long id) {
        return ResponseEntity.ok(showService.toggleShowActive(id));
    }
    
    // Book seats temporarily (for seat selection)
    @PostMapping("/{showId}/book-seats")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> bookSeats(
            @PathVariable Long showId,
            @RequestBody SeatSelectionRequest request) {
        showService.updateBookedSeats(showId, request.getSeatNumbers());
        return ResponseEntity.ok(Map.of("message", "Seats booked successfully"));
    }
    
    // Release seats (for cancellation)
    @PostMapping("/{showId}/release-seats")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> releaseSeats(
            @PathVariable Long showId,
            @RequestBody SeatSelectionRequest request) {
        showService.releaseBookedSeats(showId, request.getSeatNumbers());
        return ResponseEntity.ok(Map.of("message", "Seats released successfully"));
    }
}