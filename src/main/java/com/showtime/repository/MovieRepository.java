package com.showtime.repository;

import com.showtime.model.Movie;
import com.showtime.model.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {
    List<Movie> findByIsActiveTrueOrderByReleaseDateDesc();
    
    @Query("SELECT m FROM Movie m WHERE m.isActive = true AND (LOWER(m.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(m.cast) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(m.director) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(m.genre) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Movie> searchMovies(@Param("query") String query);
    
    @Query("Select distinct m from movie m left join m.shows s where m.isActive = true "
    		+ "and (:language is null OR lower(m.language) like lower(concat('%',:language,'%'))) "
    		+ "and (:genre is null or lower(m.genre) like lower(concat('%',:genre,'%'))) "
    		+ "and (:city is null or s is not null and lower(s.theater.city) like lower(concat('%',:city,'%')))")
    List<Movie> findMoviesWithFilters(
        @Param("date") LocalDate date,
        @Param("city") String city,
        @Param("language") String language,
        @Param("genre") String genre
    );
    
    @Query("SELECT m FROM Movie m WHERE m.releaseDate > CURRENT_DATE ORDER BY m.releaseDate")
    List<Movie> findUpcomingMovies();
    
    List<Movie> findByGenreContainingIgnoreCase(String genre);
    List<Movie> findByLanguageContainingIgnoreCase(String language);
    
    @Query("SELECT COUNT(m) FROM Movie m WHERE m.isActive = true")
    Long countActiveMovies();
    
    // Count upcoming movies (release date > today)
    @Query("SELECT COUNT(m) FROM Movie m WHERE m.releaseDate > CURRENT_DATE")
    Long countUpcomingMovies();
    
    // Search users by name or email
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<User> searchUsers(@Param("query") String query);
    
    @Query("SELECT DISTINCT m FROM Movie m " +
            "LEFT JOIN m.shows s " +
            "WHERE m.isActive = true " +
            "AND (:language IS NULL OR LOWER(m.language) LIKE LOWER(CONCAT('%', :language, '%'))) " +
            "AND (:genre IS NULL OR LOWER(m.genre) LIKE LOWER(CONCAT('%', :genre, '%'))) " +
            "AND (:city IS NULL OR (s IS NOT NULL AND LOWER(s.theater.city) LIKE LOWER(CONCAT('%', :city, '%')))) " +
            "AND (:date IS NULL OR (s IS NOT NULL AND s.showDate >= :date)) " +
            "ORDER BY m.releaseDate asc")
     Page<Movie> findMoviesWithFilters(
         @Param("date") LocalDate date,
         @Param("city") String city,
         @Param("language") String language,
         @Param("genre") String genre,
         Pageable pageable
     );
     
     // ALTERNATIVE SIMPLER VERSION (without city filter in JOIN)
     @Query("SELECT m FROM Movie m WHERE m.isActive = true " +
            "AND (:language IS NULL OR LOWER(m.language) LIKE LOWER(CONCAT('%', :language, '%'))) " +
            "AND (:genre IS NULL OR LOWER(m.genre) LIKE LOWER(CONCAT('%', :genre, '%'))) " +
            "ORDER BY m.releaseDate DESC")
     Page<Movie> findMoviesByLanguageAndGenre(
         @Param("language") String language,
         @Param("genre") String genre,
         Pageable pageable
     );    
    
    
    
    
    
    
}