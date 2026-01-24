package com.showtime.repository;

import com.showtime.model.Screen;
import com.showtime.model.Theater;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScreenRepository extends JpaRepository<Screen, Long> {
    
    List<Screen> findByTheaterId(Long theaterId);
    
    List<Screen> findByTheaterIdAndIsActiveTrue(Long theaterId);
    
    Optional<Screen> findByTheaterIdAndScreenNumber(Long theaterId, Integer screenNumber);
    
    @Query("SELECT s FROM Screen s WHERE s.theater.id = :theaterId AND s.isActive = true ORDER BY s.screenNumber")
    List<Screen> findActiveScreensByTheater(@Param("theaterId") Long theaterId);
    
    @Query("SELECT COUNT(s) FROM Screen s WHERE s.theater.id = :theaterId AND s.isActive = true")
    Integer countActiveScreensByTheater(@Param("theaterId") Long theaterId);
    
    @Query("SELECT s FROM Screen s WHERE s.theater.id = :theaterId AND s.screenName LIKE %:name%")
    List<Screen> findByTheaterAndScreenNameContaining(@Param("theaterId") Long theaterId, 
                                                      @Param("name") String name);
    
    @Query("SELECT s FROM Screen s WHERE s.theater.city = :city AND s.isActive = true")
    List<Screen> findScreensByCity(@Param("city") String city);
    
    @Query("SELECT s FROM Screen s WHERE s.theater = :theater AND s.screenNumber = :screenNumber")
    Optional<Screen> findByTheaterAndScreenNumber(
        @Param("theater") Theater theater,
        @Param("screenNumber") Integer screenNumber
    );

    
}