package com.showtime.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "movies")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Movie {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(length = 2000)
    private String description;
    
    @Column(length = 2000)
    private String cast;
    
    @Column(length = 2000)
    private String director;
    
    private Integer duration; // in minutes
    
    private String language;
    
    private String genre;
    
    // FIX HERE: Remove precision/scale for Double
    private Double rating = 0.0;
    
    @Column(name = "release_date")
    private LocalDate releaseDate;
    
    @Column(name = "poster_url")
    private String posterUrl;
    
    @Column(name = "trailer_url")
    private String trailerUrl;
    
    @Column(name = "banner_url")
    private String bannerUrl;
    
    @Column(name = "is_active")
    private boolean isActive = true;
    
    @Enumerated(EnumType.STRING)
    private Certification certification = Certification.UA;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Show> shows = new ArrayList<>();
    
    @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Review> reviews = new ArrayList<>();
    
    @ManyToMany(mappedBy = "wishlist", fetch = FetchType.LAZY)
    private Set<User> users = new HashSet<>();
    
    public enum Certification {
        U, UA, A, S
    }
}