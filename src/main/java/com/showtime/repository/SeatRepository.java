package com.showtime.repository;

import com.showtime.model.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.showtime.model.Seat.SeatStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
	
    @Modifying
    @Query("UPDATE Seat s SET s.status = 'AVAILABLE', s.booking = null " +
           "WHERE s.booking.id = :bookingId")
    int releaseSeats(@Param("bookingId") Long bookingId);
    
    long countByShowIdAndStatus(Long showId, SeatStatus status);
    
    // Find seats by show ID
    List<Seat> findByShowId(Long showId);
    
    // Find seats by show ID and seat numbers
    @Query("SELECT s FROM Seat s WHERE s.show.id = :showId AND s.seatNumber IN :seatNumbers")
    List<Seat> findByShowIdAndSeatNumberIn(@Param("showId") Long showId, 
                                          @Param("seatNumbers") List<String> seatNumbers);
    
    // Find seats by booking ID
    List<Seat> findByBookingId(Long bookingId);
    
    // Find seat by show ID and seat number
    Optional<Seat> findByShowIdAndSeatNumber(Long showId, String seatNumber);
    
    // Count available seats for a show
    @Query("SELECT COUNT(s) FROM Seat s WHERE s.show.id = :showId AND s.status = 'AVAILABLE'")
    Long countByShowIdAndStatus(@Param("showId") Long showId);
    
    // Find all booked seats for a show
    @Query("SELECT s FROM Seat s WHERE s.show.id = :showId AND s.status = 'BOOKED'")
    List<Seat> findBookedSeatsByShowId(@Param("showId") Long showId);
}