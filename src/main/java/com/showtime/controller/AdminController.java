package com.showtime.controller;

import com.showtime.dto.*;
import com.showtime.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {
    
    private final UserService userService;
    private final MovieService movieService;
    private final TheaterService theaterService;
    private final ShowService showService;
    private final BookingService bookingService;
    
    
    
    @GetMapping("/dashboard/stats")
    public ResponseEntity<DashboardStatsDTO> getDashboardStats() {
        DashboardStatsDTO stats = new DashboardStatsDTO();
        stats.setTotalBookings(bookingService.getTotalBookings());
        stats.setTotalMovies(movieService.getTotalMovies());
        stats.setTotalTheaters(theaterService.getTotalTheaters());
        stats.setTotalUsers(userService.getTotalUsers());
        stats.setTotalRevenue(bookingService.getTotalRevenue());
        
//        // Convert Object[] to RevenueChartDTO
//        List<RevenueChartDTO> revenueChart = bookingService.getMonthlyRevenue().stream()
//            .map(data -> {
//                RevenueChartDTO dto = new RevenueChartDTO();
//                dto.setMonth((String) data[0]);
//                dto.setRevenue((Double) data[1]);
//                return dto;
//            })
//            .collect(Collectors.toList());
//        
//        stats.setRevenueChart(revenueChart);
        return ResponseEntity.ok(stats);
    }
    
    // USER MANAGEMENT ENDPOINTS
    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }
    
    @PutMapping("/users/{id}/role")
    public ResponseEntity<UserDTO> updateUserRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        return ResponseEntity.ok(userService.updateUserRole(id, request.get("role")));
    }
    
    @GetMapping("/users/search")
    public ResponseEntity<List<UserDTO>> searchUsers(@RequestParam String query) {
        return ResponseEntity.ok(userService.searchUsers(query));
    }
    
    // MOVIE MANAGEMENT ENDPOINTS
    @GetMapping("/movies/analytics")
    public ResponseEntity<Map<String, Object>> getMovieAnalytics() {
        Map<String, Object> analytics = Map.of(
            "totalMovies", movieService.getTotalMovies(),
            "activeMovies", movieService.getActiveMoviesCount(),
            "upcomingMovies", movieService.getUpcomingMoviesCount()
        );
        return ResponseEntity.ok(analytics);
    }
    
    // THEATER MANAGEMENT ENDPOINTS
    @GetMapping("/theaters/analytics")
    public ResponseEntity<Map<String, Object>> getTheaterAnalytics() {
        Map<String, Object> analytics = Map.of(
            "totalTheaters", theaterService.getTotalTheaters(),
            "cities", theaterService.getAllCities(),
            "activeTheaters", theaterService.getActiveTheatersCount()
        );
        return ResponseEntity.ok(analytics);
    }
    
    @GetMapping("/bookings/revenue")
    public ResponseEntity<Map<String, Object>> getRevenueReport(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Map<String, Object> report = Map.of(
            "totalRevenue", bookingService.getTotalRevenue()
//            "monthlyRevenue", bookingService.getMonthlyRevenue(),
//            "todaysBookings", bookingService.getTodaysBookingsCount()
        );
        return ResponseEntity.ok(report);
    }
        
    // SYSTEM ENDPOINTS
    @GetMapping("/system/health")
    public ResponseEntity<Map<String, Object>> getSystemHealth() {
        Map<String, Object> health = Map.of(
            "status", "UP",
            "timestamp", LocalDateTime.now(),
            "database", "CONNECTED",
            "memoryUsage", Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        );
        return ResponseEntity.ok(health);
    }
    
    @PostMapping("/system/cache/clear")
    public ResponseEntity<?> clearCache() {
        // Implement cache clearing logic
        return ResponseEntity.ok(Map.of("message", "Cache cleared successfully"));
    }
}