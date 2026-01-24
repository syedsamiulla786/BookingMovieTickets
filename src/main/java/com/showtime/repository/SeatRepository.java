package com.showtime.repository;

import com.showtime.model.Seat;
import com.showtime.model.Seat.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
    
    List<Seat> findByShowId(Long showId);
    
    List<Seat> findByShowIdAndStatus(Long showId, SeatStatus status);
    
    List<Seat> findByShowIdAndSeatNumberIn(Long showId, List<String> seatNumbers);
    
    @Query("SELECT s FROM Seat s WHERE s.show.id = :showId AND s.seatNumber IN :seatNumbers")
    List<Seat> findSeatsForBooking(@Param("showId") Long showId, 
                                   @Param("seatNumbers") List<String> seatNumbers);
    
 // Update the lockSeats query
    @Modifying
    @Query("UPDATE Seat s SET s.status = 'LOCKED', s.lockedUntil = :lockedUntil, s.lockedBy = :userId " +
           "WHERE s.show.id = :showId AND s.seatNumber IN :seatNumbers AND s.status = 'AVAILABLE'")
    int lockSeats(@Param("showId") Long showId,
                  @Param("seatNumbers") List<String> seatNumbers,
                  @Param("userId") Long userId,
                  @Param("lockedUntil") LocalDateTime lockedUntil);

    
    @Modifying
    @Query("UPDATE Seat s SET s.status = 'AVAILABLE', s.lockedUntil = null, s.lockedBy = null " +
           "WHERE s.lockedBy = :userId AND s.lockedUntil < :now")
    int releaseExpiredLocks(@Param("userId") Long userId, @Param("now") LocalDateTime now);
    
    @Modifying
    @Query("UPDATE Seat s SET s.status = 'BOOKED', s.booking.id = :bookingId " +
           "WHERE s.show.id = :showId AND s.seatNumber IN :seatNumbers")
    int bookSeats(@Param("showId") Long showId,
                  @Param("seatNumbers") List<String> seatNumbers,
                  @Param("bookingId") Long bookingId);
    
    @Modifying
    @Query("UPDATE Seat s SET s.status = 'AVAILABLE', s.booking = null " +
           "WHERE s.booking.id = :bookingId")
    int releaseSeats(@Param("bookingId") Long bookingId);
    
    long countByShowIdAndStatus(Long showId, SeatStatus status);
    
    @Modifying
    @Query("DELETE FROM Seat s WHERE s.show.id = :showId")
    void deleteByShowId(@Param("showId") Long showId);
}