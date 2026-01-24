package com.showtime.repository;

import com.showtime.model.Theater;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TheaterRepository extends JpaRepository<Theater, Long> {
    List<Theater> findByCityIgnoreCaseAndIsActiveTrue(String city);
    
    @Query("SELECT DISTINCT t.city FROM Theater t WHERE t.isActive = true ORDER BY t.city")
    List<String> findAllCities();
    
    @Query("SELECT t FROM Theater t WHERE t.isActive = true AND " +
           "(LOWER(t.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(t.city) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(t.address) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Theater> searchTheaters(@Param("query") String query);
    
    @Query("SELECT COUNT(t) FROM Theater t WHERE t.isActive = true")
    Long countActiveTheaters();
    
    Long countByIsActiveTrue();
}