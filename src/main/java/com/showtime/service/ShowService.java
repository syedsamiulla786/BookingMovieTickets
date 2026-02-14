 package com.showtime.service;

import com.showtime.dto.*;
import com.showtime.dto.request.ShowRequest;
import com.showtime.dto.response.SeatLayoutResponse;
import com.showtime.exception.ResourceNotFoundException;
import com.showtime.model.*;
import com.showtime.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShowService {
    
    private final ShowRepository showRepository;
    private final MovieRepository movieRepository;
    private final TheaterRepository theaterRepository;
    private final ScreenRepository screenRepository;
    private final SeatRepository seatRepository;
    
    // Get shows by movie and city
    public List<ShowDTO> getShowsByMovie(Long movieId, String city) {
        LocalDate today = LocalDate.now();
        return showRepository.findShowsByMovieAndCity(movieId, today, city).stream()
            .map(show->convertToDTO(show))
            .collect(Collectors.toList());
    }
    
    // Get available dates for a movie
    public List<LocalDate> getAvailableDatesForMovie(Long movieId) {
        return showRepository.findAvailableDatesForMovie(movieId);
    }
    
    // Get shows by theater and date
    public List<ShowDTO> getShowsByTheater(Long theaterId, LocalDate date) {
        if (date == null) {
            date = LocalDate.now() ;
        }
        return showRepository.findByTheaterIdAndShowDateAndIsActiveTrueOrderByShowTime(theaterId,date).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    // Get show by ID
    public ShowDTO getShowById(Long id) {
    	Show show = showRepository.findById(id).orElseThrow(()->new RuntimeException("Show Not Found"));
    	return convertToDTO(show);
    }
    
    // Create new show
    @Transactional
    public ShowDTO createShow(ShowRequest request) {
        // Validate and get entities
        Movie movie = movieRepository.findById(request.getMovieId())
            .orElseThrow(() -> new ResourceNotFoundException("Movie not found"));
        Theater theater = theaterRepository.findById(request.getTheaterId())
            .orElseThrow(() -> new ResourceNotFoundException("Theater not found"));
        
        // Get or create screen
        Screen screen = screenRepository.findByTheaterAndScreenNumber(theater, request.getScreenNumber())
            .orElseGet(() -> createScreen(theater, request.getScreenNumber()));
        
        // Create show
        Show show = new Show();
        show.setMovie(movie);
        show.setTheater(theater);
        show.setScreen(screen);
        show.setShowDate(request.getShowDate());
        show.setShowTime(request.getShowTime());
        show.setPriceClassic(request.getPriceClassic() != null ? request.getPriceClassic() : 200.0);
        show.setPricePremium(request.getPricePremium() != null ? request.getPricePremium() : 350.0);
        show.setActive(true);
        
        // Calculate end time
        if (movie.getDuration() != null && request.getShowTime() != null) {
            show.setEndTime(request.getShowTime().plusMinutes(movie.getDuration()));
        }
        
        // Save show
        Show savedShow = showRepository.save(show);
        
        // Create seats for this show
        createSeatsForShow(savedShow, screen);
        
        return convertToDTO(savedShow);
    }
    
    // Create screen if not exists
    private Screen createScreen(Theater theater, Integer screenNumber) {
        Screen screen = new Screen();
        screen.setTheater(theater);
        screen.setScreenNumber(screenNumber);
        screen.setScreenName("Screen " + screenNumber);
        screen.setTotalSeats(theater.getSeatsPerScreen() != null ? theater.getSeatsPerScreen() : 100);
        screen.setActive(true);
        return screenRepository.save(screen);
    }
    
    // Create seats for a show
    @Transactional
    private void createSeatsForShow(Show show, Screen screen) {
        List<Seat> seats = new ArrayList<>();
        int totalRows = 5; // 5 rows: A, B, C, D, E
        int seatsPerRow = screen.getTotalSeats() / totalRows;
        
        for (int row = 0; row < totalRows; row++) {
            for (int seatNum = 1; seatNum <= seatsPerRow; seatNum++) {
                String seatNumber = String.format("%c%d", 'A' + row, seatNum);
                
                Seat seat = new Seat();
                seat.setShow(show);
                seat.setSeatNumber(seatNumber);
                seat.setSeatRow(String.valueOf(('A' + row)));
                seat.setSeatColumn(seatNum);
                
                // First 2 rows are premium, rest are classic
                if (row < 2) {
                    seat.setSeatType(Seat.SeatType.PREMIUM);
                    seat.setPrice(show.getPricePremium());
                } else {
                    seat.setSeatType(Seat.SeatType.CLASSIC);
                    seat.setPrice(show.getPriceClassic());
                }
                
                seat.setStatus(Seat.SeatStatus.AVAILABLE);
                seats.add(seat);
            }
        }
        
        seatRepository.saveAll(seats);
    }
    
    // Get seat layout for a show
    public SeatLayoutResponse getSeatLayout(Long showId) {
        Show show = showRepository.findById(showId)
            .orElseThrow(() -> new RuntimeException("Show not found"));
        
        // Get all seats for this show
        List<Seat> seats = seatRepository.findByShowId(showId);
        
        System.out.println("SHow Id : "+showId);
//        System.out.println("seats : "+seats);
        
        // Create response
        SeatLayoutResponse response = new SeatLayoutResponse();
        response.setShowId(showId);
        response.setClassicPrice(show.getPriceClassic());
        response.setPremiumPrice(show.getPricePremium());
        
        // Convert seats to DTOs and organize by row
        List<List<SeatDTO>> seatLayout = new ArrayList<>();
        
        // Group seats by row
        seats.stream()
            .collect(Collectors.groupingBy(Seat::getSeatRow))// to Map
            .forEach((row, rowSeats) -> { // itr Map
                List<SeatDTO> seatDTOs = rowSeats.stream()
                    .sorted((s1, s2) -> s1.getSeatColumn() - s2.getSeatColumn())
                    .map(this::convertSeatToDTO)
                    .collect(Collectors.toList());
                seatLayout.add(seatDTOs);
            });
        
        // Sort rows alphabetically (A, B, C, etc.)
        seatLayout.sort((row1, row2) -> {
            if (row1.isEmpty() || row2.isEmpty()) return 0;
            return row1.get(0).getSeatNumber().charAt(0) - row2.get(0).getSeatNumber().charAt(0);
        });
        
        response.setSeatLayout(seatLayout);
        response.setTotalSeats(seats.size());
        
        // Calculate available seats
        long availableSeats = seats.stream()
            .filter(seat -> seat.getStatus() == Seat.SeatStatus.AVAILABLE)
            .count();
        response.setAvailableSeats((int) availableSeats);
        
        // Get booked seat numbers
        List<String> bookedSeats = seats.stream()
            .filter(seat -> seat.getStatus() == Seat.SeatStatus.BOOKED)
            .map(Seat::getSeatNumber)
            .collect(Collectors.toList());
        response.setBookedSeats(bookedSeats);
        
        return response;
    }
    
    // Convert Seat to SeatDTO
    private SeatDTO convertSeatToDTO(Seat seat) {
        SeatDTO dto = new SeatDTO();
        dto.setId(seat.getId());
        dto.setSeatNumber(seat.getSeatNumber());
        dto.setSeatType(seat.getSeatType().name());
        dto.setPrice(seat.getPrice());
        dto.setStatus(seat.getStatus().name());
        dto.setIsAvailable(seat.getStatus() == Seat.SeatStatus.AVAILABLE);
        dto.setRow(seat.getSeatRow().charAt(0) - 'A'); // Convert A->0, B->1, etc.
        dto.setColumn(seat.getSeatColumn() - 1); // Convert to 0-based
        dto.setDisplayName(seat.getSeatNumber());
        return dto;
    }
    
    // Convert Show to ShowDTO
    private ShowDTO convertToDTO(Show show) {
        ShowDTO dto = new ShowDTO();
        dto.setId(show.getId());
        dto.setShowDate(show.getShowDate());
        dto.setShowTime(show.getShowTime());
        dto.setEndTime(show.getEndTime());
        dto.setPriceClassic(show.getPriceClassic());
        dto.setPricePremium(show.getPricePremium());
        dto.setActive(show.isActive());
        
        // Calculate available seats
        long availableSeats = seatRepository.countByShowIdAndStatus(show.getId(), Seat.SeatStatus.AVAILABLE);
        dto.setAvailableSeats((int) availableSeats);
        
        if (show.getMovie() != null) {
            MovieDTO movieDTO = new MovieDTO();
            movieDTO.setId(show.getMovie().getId());
            movieDTO.setTitle(show.getMovie().getTitle());
            movieDTO.setDuration(show.getMovie().getDuration());
            movieDTO.setLanguage(show.getMovie().getLanguage());
            dto.setMovie(movieDTO);
        }
        
        if (show.getTheater() != null) {
            TheaterDTO theaterDTO = new TheaterDTO();
            theaterDTO.setId(show.getTheater().getId());
            theaterDTO.setName(show.getTheater().getName());
            theaterDTO.setCity(show.getTheater().getCity());
            dto.setTheater(theaterDTO);
        }
        
        if (show.getScreen() != null) {
            ScreenDTO screenDTO = new ScreenDTO();
            screenDTO.setId(show.getScreen().getId());
            screenDTO.setScreenNumber(show.getScreen().getScreenNumber());
            screenDTO.setScreenName(show.getScreen().getScreenName());
            screenDTO.setTotalSeats(show.getScreen().getTotalSeats());
            dto.setScreen(screenDTO);
        }
        
        return dto;
    }
    
    // Update show
    @Transactional
    public ShowDTO updateShow(Long id, ShowRequest request) {
        Show show = showRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Show not found"));
        
        if (request.getMovieId() != null) {
            Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new RuntimeException("Movie not found"));
            show.setMovie(movie);
        }
        
        if (request.getTheaterId() != null) {
            Theater theater = theaterRepository.findById(request.getTheaterId())
                .orElseThrow(() -> new RuntimeException("Theater not found"));
            show.setTheater(theater);
        }
        
        if (request.getScreenNumber() != null) {
            Screen screen = screenRepository.findByTheaterAndScreenNumber(
                    show.getTheater(), request.getScreenNumber())
                .orElseThrow(() -> new RuntimeException("Screen not found"));
            show.setScreen(screen);
        }
        
        if (request.getShowDate() != null) {
            show.setShowDate(request.getShowDate());
        }
        
        if (request.getShowTime() != null) {
            show.setShowTime(request.getShowTime());
        }
        
        if (request.getPriceClassic() != null) {
            show.setPriceClassic(request.getPriceClassic());
        }
        
        if (request.getPricePremium() != null) {
            show.setPricePremium(request.getPricePremium());
        }
        
        Show updatedShow = showRepository.save(show);
        return convertToDTO(updatedShow);
    }
    
    // Delete show
    @Transactional
    public void deleteShow(Long id) {
        // Delete all seats first
        List<Seat> seats = seatRepository.findByShowId(id);
        seatRepository.deleteAll(seats);
        
        // Then delete show
        showRepository.deleteById(id);
    }
    
    // Toggle show active status
    @Transactional
    public ShowDTO toggleShowActive(Long id) {
        Show show = showRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Show not found"));
        
        show.setActive(!show.isActive());
        Show updatedShow = showRepository.save(show);
        return convertToDTO(updatedShow);
    }
    
    // Get all shows
    public List<ShowDTO> getAllShows() {
        return showRepository.findAll().stream()
            .map(show->convertToDTO(show))
            .collect(Collectors.toList());
    }
    
    // Get total active shows
    public Long getTotalShows() {
        return showRepository.countActiveShows();
    }
}