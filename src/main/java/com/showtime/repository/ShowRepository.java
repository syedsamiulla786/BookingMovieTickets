package com.showtime.repository;

import com.showtime.model.Show;
import com.showtime.model.Theater;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShowRepository extends JpaRepository<Show, Long> {
    
    // Find shows by movie and date
    List<Show> findByMovieIdAndShowDateAndIsActiveTrueOrderByShowTime(Long movieId, LocalDate showDate);
    
    // Find shows by theater and date
    List<Show> findByTheaterIdAndShowDateAndIsActiveTrueOrderByShowTime(Long theaterId, LocalDate showDate);
    
    // Find shows by movie and city
    @Query("SELECT s FROM Show s WHERE s.movie.id = :movieId " +
           "AND s.showDate >= :date AND s.isActive = true " +
           "AND (:city IS NULL OR LOWER(s.theater.city) LIKE LOWER(CONCAT('%', :city, '%'))) " +
           "ORDER BY s.showDate, s.showTime")
    List<Show> findShowsByMovieAndCity(
        @Param("movieId") Long movieId,
        @Param("date") LocalDate date,
        @Param("city") String city
    );
    
    // Get available dates for a movie
    @Query("SELECT DISTINCT s.showDate FROM Show s WHERE s.movie.id = :movieId " +
           "AND s.showDate >= CURRENT_DATE AND s.isActive = true " +
           "ORDER BY s.showDate")
    List<LocalDate> findAvailableDatesForMovie(@Param("movieId") Long movieId);
    
    // Find upcoming shows for a date
    @Query("SELECT s FROM Show s WHERE s.showDate = :date " +
           "AND s.showTime > :time AND s.isActive = true " +
           "ORDER BY s.showTime")
    List<Show> findUpcomingShows(@Param("date") LocalDate date, 
                                @Param("time") LocalTime time);
    
    // Count active shows
    @Query("SELECT COUNT(s) FROM Show s WHERE s.isActive = true")
    Long countActiveShows();
    
    // Find shows by screen
    List<Show> findByScreenIdAndIsActiveTrue(Long screenId);
    
    // Find show by theater, screen, date and time
    Optional<Show> findByTheaterAndScreenAndShowDateAndShowTime(
        Theater theater, com.showtime.model.Screen screen, 
        LocalDate showDate, LocalTime showTime
    );
}