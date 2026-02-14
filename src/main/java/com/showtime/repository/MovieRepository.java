package com.showtime.repository;

import com.showtime.model.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {
    
    List<Movie> findByIsActiveTrueOrderByReleaseDateDesc();
    
    @Query("SELECT m FROM Movie m WHERE m.isActive = true AND " +
           "(LOWER(m.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(m.cast) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(m.director) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(m.genre) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Movie> searchMovies(@Param("query") String query);
    
    @Query("SELECT m FROM Movie m WHERE m.releaseDate > CURRENT_DATE AND m.isActive = true ORDER BY m.releaseDate")
    List<Movie> findUpcomingMovies();
    
    @Query("SELECT COUNT(m) FROM Movie m WHERE m.isActive = true")
    Long countActiveMovies();
    
    @Query("SELECT COUNT(m) FROM Movie m WHERE m.releaseDate > CURRENT_DATE AND m.isActive = true")
    Long countUpcomingMovies();
    
    // METHOD 1: Filter by language and genre only (NO city/date)
    @Query("SELECT m FROM Movie m WHERE m.isActive = true " +
           "AND (:language IS NULL OR :language = '' OR LOWER(m.language) LIKE LOWER(CONCAT('%', :language, '%'))) " +
           "AND (:genre IS NULL OR :genre = '' OR LOWER(m.genre) LIKE LOWER(CONCAT('%', :genre, '%')))")
    Page<Movie> findMoviesByFilters(
            @Param("language") String language,
            @Param("genre") String genre,
            Pageable pageable);
    
    // METHOD 2: Get movies that have shows in a specific city on/after a date
    @Query("SELECT DISTINCT m FROM Movie m " +
           "JOIN m.shows s " +
           "JOIN s.theater t " +
           "WHERE m.isActive = true " +
           "AND s.isActive = true " +
           "AND t.isActive = true " +
           "AND (:city IS NULL OR :city = '' OR LOWER(t.city) LIKE LOWER(CONCAT('%', :city, '%'))) " +
           "AND (:date IS NULL OR s.showDate >= :date) " +
           "AND (:language IS NULL OR :language = '' OR LOWER(m.language) LIKE LOWER(CONCAT('%', :language, '%'))) " +
           "AND (:genre IS NULL OR :genre = '' OR LOWER(m.genre) LIKE LOWER(CONCAT('%', :genre, '%')))")
    Page<Movie> findMoviesWithShowsByFilters(
            @Param("city") String city,
            @Param("date") LocalDate date,
            @Param("language") String language,
            @Param("genre") String genre,
            Pageable pageable);
    
    // METHOD 3: Get all active movies with pagination
    @Query("SELECT m FROM Movie m WHERE m.isActive = true")
    Page<Movie> findAllActiveMovies(Pageable pageable);
}