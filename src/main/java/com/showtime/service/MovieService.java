package com.showtime.service;

import com.showtime.dto.*;

import com.showtime.dto.response.*;
import com.showtime.dto.request.*;
import com.showtime.model.Movie;
import com.showtime.repository.MovieRepository;
import com.showtime.repository.ShowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;

@Service
@RequiredArgsConstructor
public class MovieService {
    
    private final MovieRepository movieRepository;
    private final ShowRepository showRepository;
    
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
            .orElseThrow(() -> new RuntimeException("Movie not found"));
        return convertToDTO(movie);
    }
    
    public List<MovieDTO> searchMovies(String query) {
        return movieRepository.searchMovies(query).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
//    public List<MovieDTO> filterMovies(MovieFilterRequest filter) {
//        LocalDate date = filter.getDate() != null ? filter.getDate() : LocalDate.now();
//        return movieRepository.findMoviesWithFilters(
//            date, filter.getCity(), filter.getLanguage(), filter.getGenre()
//        ).stream()
//            .map(this::convertToDTO)
//            .collect(Collectors.toList());
//    }
    
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
            .orElseThrow(() -> new RuntimeException("Movie not found"));
        updateMovieFromRequest(movie, request);
        movie = movieRepository.save(movie);
        return convertToDTO(movie);
    }
    
    @Transactional
    public void deleteMovie(Long id) {
        Movie movie = movieRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Movie not found"));
        movie.setActive(false);
        movieRepository.save(movie);
    }
    
    public Long getTotalMovies() {
        return movieRepository.countActiveMovies();
    }
    
 // Get count of active movies
    public Long getActiveMoviesCount() {
        return movieRepository.countActiveMovies();
    }
    
    // Get count of upcoming movies
    public Long getUpcomingMoviesCount() {
        return movieRepository.countUpcomingMovies();
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
        dto.setRating(movie.getRating());
        dto.setReleaseDate(movie.getReleaseDate());
        dto.setPosterUrl(movie.getPosterUrl());
        dto.setTrailerUrl(movie.getTrailerUrl());
        dto.setBannerUrl(movie.getBannerUrl());
        dto.setCertification(movie.getCertification().name());
        dto.setActive(movie.isActive());
        return dto;
    }
    
    public Page<MovieDTO> filterMovies(MovieFilterRequest filter) {
    	
    	System.out.println("Filtered Movies "+filter);
        Pageable pageable = PageRequest.of(
            filter.getPage(), 
            filter.getSize(),
            Sort.by(
                filter.getSortOrder().equalsIgnoreCase("desc") ? 
                    Sort.Direction.DESC : Sort.Direction.ASC, 
                filter.getSortBy()
            )
        );
        
        LocalDate date = filter.getDate() != null ? filter.getDate() : LocalDate.now();
        
        return movieRepository.findMoviesWithFilters(
            date, 
            filter.getCity(), 
            filter.getLanguage(), 
            filter.getGenre(),
            pageable
        ).map(this::convertToDTO);
    }
}