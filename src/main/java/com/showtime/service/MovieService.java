package com.showtime.service;

import com.showtime.dto.MovieDTO;
import com.showtime.dto.request.MovieFilterRequest;
import com.showtime.dto.request.MovieRequest;
import com.showtime.exception.ResourceNotFoundException;
import com.showtime.model.Movie;
import com.showtime.repository.MovieRepository;
import com.showtime.repository.ShowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MovieService {
    
    private final MovieRepository movieRepository;
    private final ShowRepository showRepository;
    private final Random random = new Random();
    
    public List<MovieDTO> getAllMovies() {
        return movieRepository.findByIsActiveTrueOrderByReleaseDateDesc().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    public List<MovieDTO> getUpcomingMovies() {
        return movieRepository.findUpcomingMovies().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    public MovieDTO getMovieById(Long id) {
        Movie movie = movieRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Movie", "id", id));
        return convertToDTO(movie);
    }
    
    public List<MovieDTO> searchMovies(String query) {
        log.info("Searching movies with query: {}", query);
        return movieRepository.searchMovies(query).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    @Transactional
    public MovieDTO createMovie(MovieRequest request) {
        Movie movie = new Movie();
        updateMovieFromRequest(movie, request);
        movie = movieRepository.save(movie);
        return convertToDTO(movie);
    }
    
    @Transactional
    public MovieDTO updateMovie(Long id, MovieRequest request) {
        Movie movie = movieRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Movie", "id", id));
        updateMovieFromRequest(movie, request);
        movie = movieRepository.save(movie);
        return convertToDTO(movie);
    }
    
    @Transactional
    public void deleteMovie(Long id) {
        Movie movie = movieRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Movie", "id", id));
        movie.setActive(false);
        movieRepository.save(movie);
    }
    
    public Long getTotalMovies() {
        return movieRepository.countActiveMovies();
    }
    
    public Long getActiveMoviesCount() {
        return movieRepository.countActiveMovies();
    }
    
    public Long getUpcomingMoviesCount() {
        return movieRepository.countUpcomingMovies();
    }
    
    /**
     * Filter movies with pagination and sorting
     * Supports filtering by language, genre, city, and date
     */
    public Page<MovieDTO> filterMovies(MovieFilterRequest filter) {
        log.info("Filtering movies with request: {}", filter);
        
        // Set defaults
        if (filter.getPage() < 0) filter.setPage(0);
        if (filter.getSize() <= 0) filter.setSize(12);
        if (filter.getSortBy() == null || filter.getSortBy().isEmpty()) {
            filter.setSortBy("releaseDate");
        }
        if (filter.getSortOrder() == null || filter.getSortOrder().isEmpty()) {
            filter.setSortOrder("desc");
        }
        
        // Create pageable
        Pageable pageable = PageRequest.of(
            filter.getPage(),
            filter.getSize(),
            Sort.by(
                filter.getSortOrder().equalsIgnoreCase("desc") ? 
                    Sort.Direction.DESC : Sort.Direction.ASC,
                getSortField(filter.getSortBy())
            )
        );
        
        Page<Movie> moviePage;
        
        // Check if we need to filter by city or date (requires joining with shows)
        boolean hasCityFilter = filter.getCity() != null && !filter.getCity().isEmpty();
        boolean hasDateFilter = filter.getDate() != null;
        
        if (hasCityFilter || hasDateFilter) {
            // Filter with city/date - requires joining with shows
            LocalDate date = filter.getDate() != null ? filter.getDate() : LocalDate.now();
            
            moviePage = movieRepository.findMoviesWithShowsByFilters(
                filter.getCity(),
                date,
                filter.getLanguage(),
                filter.getGenre(),
                pageable
            );
        } else {
            // Simple filter by language and genre only
            moviePage = movieRepository.findMoviesByFilters(
                filter.getLanguage(),
                filter.getGenre(),
                pageable
            );
        }
        
        return moviePage.map(this::convertToDTO);
    }
    
    /**
     * Convert sort field string to actual entity field name
     */
    private String getSortField(String sortBy) {
        switch (sortBy.toLowerCase()) {
            case "title":
                return "title";
            case "rating":
                return "rating";
            case "duration":
                return "duration";
            case "releaseDate":
            default:
                return "releaseDate";
        }
    }
    
    private void updateMovieFromRequest(Movie movie, MovieRequest request) {
        movie.setTitle(request.getTitle());
        movie.setDescription(request.getDescription());
        movie.setCast(request.getCast());
        movie.setDirector(request.getDirector());
        movie.setDuration(request.getDuration());
        movie.setLanguage(request.getLanguage());
        movie.setGenre(request.getGenre());
        movie.setReleaseDate(request.getReleaseDate());
        movie.setPosterUrl(request.getPosterUrl());
        movie.setTrailerUrl(request.getTrailerUrl());
        movie.setBannerUrl(request.getBannerUrl());
        if (request.getCertification() != null) {
            movie.setCertification(Movie.Certification.valueOf(request.getCertification()));
        }
    }
    
    private MovieDTO convertToDTO(Movie movie) {
        MovieDTO dto = new MovieDTO();
        dto.setId(movie.getId());
        dto.setTitle(movie.getTitle());
        dto.setDescription(movie.getDescription());
        dto.setCast(movie.getCast());
        dto.setDirector(movie.getDirector());
        dto.setDuration(movie.getDuration());
        dto.setLanguage(movie.getLanguage());
        dto.setGenre(movie.getGenre());
        
        // Generate a random rating for demo purposes
        // In production, this should come from actual reviews
        double rating = 5.8 + (random.nextDouble() * 4.2);
        dto.setRating(Math.round(rating * 10) / 10.0);
        
        dto.setReleaseDate(movie.getReleaseDate());
        dto.setPosterUrl(movie.getPosterUrl());
        dto.setTrailerUrl(movie.getTrailerUrl());
        dto.setBannerUrl(movie.getBannerUrl());
        if (movie.getCertification() != null) {
            dto.setCertification(movie.getCertification().name());
        }
        dto.setActive(movie.isActive());
        return dto;
    }
}