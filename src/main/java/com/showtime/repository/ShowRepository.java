package com.showtime.repository;

import com.showtime.model.Screen;
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
    List<Show> findByMovieIdAndShowDateAndIsActiveTrueOrderByShowTime(Long movieId, LocalDate showDate);
    
    List<Show> findByTheaterIdAndShowDateAndIsActiveTrueOrderByShowTime(Long theaterId, LocalDate showDate);
    
    @Query("SELECT s FROM Show s WHERE s.movie.id = :movieId AND s.showDate >= :date AND s.isActive = true " +
           "AND (:city IS NULL OR LOWER(s.theater.city) LIKE LOWER(CONCAT('%', :city, '%')))")
    List<Show> findShowsByMovieAndCity(
        @Param("movieId") Long movieId,
        @Param("date") LocalDate date,
        @Param("city") String city
    );
    
    @Query("SELECT DISTINCT s.showDate FROM Show s WHERE s.movie.id = :movieId " +
           "AND s.showDate >= CURRENT_DATE AND s.isActive = true ORDER BY s.showDate")
    List<LocalDate> findAvailableDatesForMovie(@Param("movieId") Long movieId);
    
    @Query("SELECT s FROM Show s WHERE s.showDate = :date AND s.showTime > :time AND s.isActive = true " +
           "ORDER BY s.showTime")
    List<Show> findUpcomingShows(@Param("date") LocalDate date, @Param("time") LocalTime time);
    
    @Query("SELECT COUNT(s) FROM Show s WHERE s.isActive = true")
    Long countActiveShows();  // Changed from getTotalShows() to countActiveShows()
  
    
}