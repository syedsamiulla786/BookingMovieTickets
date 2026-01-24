package com.showtime.repository;

import com.showtime.model.Booking;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserIdOrderByBookedAtDesc(Long userId);
    
    List<Booking> findByUserIdAndBookingStatusOrderByBookedAtDesc(Long userId, String bookingStatus);
    
    Optional<Booking> findByBookingReference(String bookingReference);
    
    @Query("SELECT b FROM Booking b WHERE b.show.showDate >= CURRENT_DATE " +
           "AND b.bookingStatus = 'CONFIRMED' AND b.user.id = :userId")
    List<Booking> findUpcomingBookings(@Param("userId") Long userId);
    
    @Query("SELECT b FROM Booking b WHERE b.show.showDate < CURRENT_DATE " +
           "AND b.bookingStatus = 'CONFIRMED' AND b.user.id = :userId")
    List<Booking> findPastBookings(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.bookingStatus = 'CONFIRMED'")
    Long countConfirmedBookings();
    
    @Query("SELECT SUM(b.totalAmount) FROM Booking b WHERE b.bookingStatus = 'CONFIRMED' " +
           "AND b.paymentStatus = 'COMPLETED'")
    Double getTotalRevenue();
    
    @Query("SELECT FUNCTION('DATE_FORMAT', b.bookedAt, '%Y-%m') as month, " +
           "SUM(b.totalAmount) as revenue FROM Booking b " +
           "WHERE b.bookedAt >= :startDate AND b.bookingStatus = 'CONFIRMED' " +
           "AND b.paymentStatus = 'COMPLETED' " +
           "GROUP BY FUNCTION('DATE_FORMAT', b.bookedAt, '%Y-%m') " +
           "ORDER BY month")
    List<Object[]> getMonthlyRevenue(@Param("startDate") LocalDateTime startDate);
     
     // Find all bookings with pagination
     Page<Booking> findAll(Pageable pageable);
     
     // Count today's bookings
     @Query("SELECT COUNT(b) FROM Booking b WHERE b.bookedAt BETWEEN :start AND :end")
     Long countByBookedAtBetween(@Param("start") LocalDateTime start, 
                                 @Param("end") LocalDateTime end);
     
     // Find bookings by status
     List<Booking> findByBookingStatusOrderByBookedAtDesc(Booking.BookingStatus status);
     
     // Find bookings within date range
     @Query("SELECT b FROM Booking b WHERE b.bookedAt BETWEEN :startDate AND :endDate " +
            "ORDER BY b.bookedAt DESC")
     List<Booking> findBookingsByDateRange(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);
     
     // Get booking statistics
     @Query("SELECT b.bookingStatus, COUNT(b) FROM Booking b GROUP BY b.bookingStatus")
     List<Object[]> getBookingStatistics();
     
     // Find bookings by payment status
     List<Booking> findByPaymentStatusOrderByBookedAtDesc(Booking.PaymentStatus paymentStatus);
    
}