package com.showtime.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.showtime.dto.*;
import com.showtime.dto.request.ShowRequest;
import com.showtime.dto.response.SeatLayoutResponse;
import com.showtime.model.*;
import com.showtime.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ShowService {
    
    private final ShowRepository showRepository;
    private final MovieRepository movieRepository;
    private final TheaterRepository theaterRepository;
    private final ScreenRepository screenRepository;
    private final ObjectMapper objectMapper;
    private final SeatService seatService;
    
    public List<ShowDTO> getShowsByMovie(Long movieId, String city) {
        LocalDate today = LocalDate.now();
        return showRepository.findShowsByMovieAndCity(movieId, today, city).stream()
            .map(this::convertToDTO)
            .collect(java.util.stream.Collectors.toList());
    }
    
    public List<ShowDTO> getAllShows() {
        return showRepository.findAll().stream()
            .map(this::convertToDTO)
            .collect(java.util.stream.Collectors.toList());
    }
    
    @Transactional
    public void deleteShow(Long id) {
        // Delete seats first
        seatService.deleteSeatsForShow(id);
        
        // Then delete show
        showRepository.deleteById(id);
    }
    
    public ShowDTO getShowById(Long id) {
        Show show = showRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Show not found with id: " + id));
        return convertToDTO(show);
    }
    
    public List<LocalDate> getAvailableDatesForMovie(Long movieId) {
        return showRepository.findAvailableDatesForMovie(movieId);
    }
    
    public List<ShowDTO> getShowsByTheater(Long theaterId, LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        return showRepository.findByTheaterIdAndShowDateAndIsActiveTrueOrderByShowTime(theaterId, date).stream()
            .map(this::convertToDTO)
            .collect(java.util.stream.Collectors.toList());
    }
    
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
            Screen screen = screenRepository.findByTheaterIdAndScreenNumber(
                    show.getTheater().getId(), request.getScreenNumber())
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
        
        show = showRepository.save(show);
        return convertToDTO(show);
    }
    
    @Transactional
    public ShowDTO toggleShowActive(Long id) {
        Show show = showRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Show not found"));
        
        show.setActive(!show.isActive());
        show = showRepository.save(show);
        return convertToDTO(show);
    }
    
    @Transactional
    public void releaseBookedSeats(Long showId, List<String> seatNumbers) {
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new RuntimeException("Show not found"));
        
        try {
            List<String> bookedSeats = objectMapper.readValue(show.getBookedSeats(), List.class);
            bookedSeats.removeAll(seatNumbers);
            show.setBookedSeats(objectMapper.writeValueAsString(bookedSeats));
            show.setAvailableSeats(show.getAvailableSeats() + seatNumbers.size());
            showRepository.save(show);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error releasing booked seats");
        }
    }
    
    public SeatLayoutResponse getSeatLayout(Long showId) {
        Show show = showRepository.findById(showId)
            .orElseThrow(() -> new RuntimeException("Show not found"));
        
        SeatLayoutResponse response = new SeatLayoutResponse();
        response.setShowId(showId);
        response.setClassicPrice(show.getPriceClassic());
        response.setPremiumPrice(show.getPricePremium());
        
        try {
            // Handle null or empty booked seats
            List<String> bookedSeats = new ArrayList<>();
            if (show.getBookedSeats() != null && !show.getBookedSeats().isEmpty()) {
                try {
                    bookedSeats = objectMapper.readValue(show.getBookedSeats(), List.class);
                } catch (JsonProcessingException e) {
                    // If JSON is invalid, treat as empty list
                    System.err.println("Invalid bookedSeats JSON for show " + showId + ": " + show.getBookedSeats());
                    bookedSeats = new ArrayList<>();
                }
            }
            
            response.setBookedSeats(bookedSeats);
            response.setBookedSeatsCount(bookedSeats.size());
            
            // Generate seat layout based on screen configuration
            Screen screen = show.getScreen();
            List<List<SeatDTO>> seatLayout = new ArrayList<>();
            
            if (screen != null && screen.getSeatsLayout() != null && !screen.getSeatsLayout().isEmpty()) {
                try {
                    // Try to use screen's seat layout configuration
                    @SuppressWarnings("unchecked")
                    List<List<Map<String, Object>>> rawLayout = objectMapper.readValue(
                        screen.getSeatsLayout(), List.class);
                    
                    seatLayout = convertRawLayoutToSeatDTOs(rawLayout, show, bookedSeats);
                    
                } catch (JsonProcessingException e) {
                    System.err.println("Invalid seatsLayout JSON for screen " + screen.getId() + 
                                     ": " + screen.getSeatsLayout());
                    // Fall back to default layout
                    seatLayout = generateDefaultSeatLayout(show, bookedSeats);
                }
            } else {
                // Generate default seat layout
                seatLayout = generateDefaultSeatLayout(show, bookedSeats);
            }
            
            // Set screen info
            if (screen != null) {
                ScreenDTO screenDTO = new ScreenDTO();
                screenDTO.setId(screen.getId());
                screenDTO.setScreenNumber(screen.getScreenNumber());
                screenDTO.setScreenName(screen.getScreenName());
                screenDTO.setTotalSeats(screen.getTotalSeats());
                response.setScreen(screenDTO);
            }
            
            // Update seat availability and set row/column positions
            int totalSeats = 0;
            int availableSeats = 0;
            
            for (int rowIndex = 0; rowIndex < seatLayout.size(); rowIndex++) {
                List<SeatDTO> row = seatLayout.get(rowIndex);
                for (int colIndex = 0; colIndex < row.size(); colIndex++) {
                    SeatDTO seat = row.get(colIndex);
                    totalSeats++;
                    
                    // Set position
                    seat.setRow(rowIndex);
                    seat.setColumn(colIndex);
                    seat.setDisplayName(seat.getSeatNumber());
                    
                    // Check if seat is available
                    boolean isAvailable = !bookedSeats.contains(seat.getSeatNumber());
                    seat.setAvailable(isAvailable);
                    seat.setSelected(false);
                    
                    // Set status based on availability
                    seat.setStatus(isAvailable ? "AVAILABLE" : "BOOKED");
                    
                    if (isAvailable) {
                        availableSeats++;
                    }
                }
            }
            
            response.setSeatLayout(seatLayout);
            response.setTotalSeats(totalSeats);
            response.setAvailableSeats(availableSeats);
            
            // Set seat prices map
            Map<String, Double> seatPrices = new HashMap<>();
            seatPrices.put("CLASSIC", show.getPriceClassic());
            seatPrices.put("PREMIUM", show.getPricePremium());
            response.setSeatPrices(seatPrices);
            
            return response;
            
        } catch (Exception e) {
            // Log the full error
            e.printStackTrace();
            throw new RuntimeException("Error processing seat layout: " + e.getMessage());
        }
    }

    private List<List<SeatDTO>> convertRawLayoutToSeatDTOs(List<List<Map<String, Object>>> rawLayout, 
                                                          Show show, List<String> bookedSeats) {
        List<List<SeatDTO>> seatLayout = new ArrayList<>();
        
        for (int rowIndex = 0; rowIndex < rawLayout.size(); rowIndex++) {
            List<Map<String, Object>> rawRow = rawLayout.get(rowIndex);
            List<SeatDTO> row = new ArrayList<>();
            
            for (int colIndex = 0; colIndex < rawRow.size(); colIndex++) {
                Map<String, Object> seatData = rawRow.get(colIndex);
                SeatDTO seat = new SeatDTO();
                
                // Extract data from the map
                seat.setSeatNumber((String) seatData.getOrDefault("seatNumber", 
                    String.format("%c%d", 'A' + rowIndex, colIndex + 1)));
                seat.setSeatType((String) seatData.getOrDefault("seatType", 
                    rowIndex < 2 ? "PREMIUM" : "CLASSIC"));
                seat.setPrice((Double) seatData.getOrDefault("price", 
                    rowIndex < 2 ? show.getPricePremium() : show.getPriceClassic()));
                
                row.add(seat);
            }
            seatLayout.add(row);
        }
        
        return seatLayout;
    }

    private List<List<SeatDTO>> generateDefaultSeatLayout(Show show, List<String> bookedSeats) {
        List<List<SeatDTO>> seatLayout = new ArrayList<>();
        int totalRows = 5;
        int seatsPerRow = 20;
        
        for (int row = 0; row < totalRows; row++) {
            List<SeatDTO> rowSeats = new ArrayList<>();
            for (int seatNum = 1; seatNum <= seatsPerRow; seatNum++) {
                String seatNumber = String.format("%c%d", 'A' + row, seatNum);
                SeatDTO seat = new SeatDTO();
                seat.setSeatNumber(seatNumber);
                
                // First 2 rows are premium
                if (row < 2) {
                    seat.setSeatType("PREMIUM");
                    seat.setPrice(show.getPricePremium());
                } else {
                    seat.setSeatType("CLASSIC");
                    seat.setPrice(show.getPriceClassic());
                }
                
                seat.setRow(row);
                seat.setColumn(seatNum - 1);
                seat.setDisplayName(seatNumber);
                rowSeats.add(seat);
            }
            seatLayout.add(rowSeats);
        }
        
        return seatLayout;
    }
    
    @Transactional
    public ShowDTO createShow(ShowRequest request) {
        Movie movie = movieRepository.findById(request.getMovieId())
            .orElseThrow(() -> new RuntimeException("Movie not found"));
        Theater theater = theaterRepository.findById(request.getTheaterId())
            .orElseThrow(() -> new RuntimeException("Theater not found"));
        
        // Find or create screen by theater and screen number
        Screen screen = screenRepository.findByTheaterAndScreenNumber(theater, request.getScreenNumber())
            .orElseGet(() -> {
                // Create new screen if it doesn't exist
                Screen newScreen = new Screen();
                newScreen.setTheater(theater);
                newScreen.setScreenNumber(request.getScreenNumber());
                newScreen.setScreenName("Screen " + request.getScreenNumber());
                newScreen.setTotalSeats(theater.getSeatsPerScreen() != null ? theater.getSeatsPerScreen() : 100);
                newScreen.setSeatsLayout(generateDefaultSeatLayoutJson(newScreen.getTotalSeats()));
                return screenRepository.save(newScreen);
            });
        
        Show show = new Show();
        show.setMovie(movie);
        show.setTheater(theater);
        show.setScreen(screen);
        show.setShowDate(request.getShowDate());
        show.setShowTime(request.getShowTime());
        
        // Calculate end time based on movie duration
        if (request.getShowTime() != null && movie.getDuration() != null) {
            LocalTime endTime = request.getShowTime().plusMinutes(movie.getDuration());
            show.setEndTime(endTime);
        }
        
        show.setPriceClassic(request.getPriceClassic());
        show.setPricePremium(request.getPricePremium());
        show.setAvailableSeats(screen.getTotalSeats());
        show.setBookedSeats("[]");
        show.setActive(true);
        
        show = showRepository.save(show);
        
        // Create seats for this show
        
        seatService.createSeatsForShow(show, screen);
        
        return convertToDTO(show);
    }

    // Helper method to generate default seat layout JSON
    private String generateDefaultSeatLayoutJson(int totalSeats) {
        try {
            int rows = 5;
            int seatsPerRow = totalSeats / rows;
            
            // Create a compact seat layout
            Map<String, Object> layout = new HashMap<>();
            layout.put("rows", rows);
            layout.put("seatsPerRow", seatsPerRow);
            layout.put("premiumRows", 2);
            
            // Create seat array with minimal data
            List<List<Map<String, Object>>> seats = new ArrayList<>();
            for (int row = 0; row < rows; row++) {
                List<Map<String, Object>> rowSeats = new ArrayList<>();
                for (int seatNum = 1; seatNum <= seatsPerRow; seatNum++) {
                    Map<String, Object> seat = new HashMap<>();
                    seat.put("seatNumber", String.format("%c%d", 'A' + row, seatNum));
                    seat.put("seatType", row < 2 ? "PREMIUM" : "CLASSIC");
                    seat.put("price", row < 2 ? 350.0 : 200.0);
                    rowSeats.add(seat);
                }
                seats.add(rowSeats);
            }
            
            layout.put("seats", seats);
            
            return objectMapper.writeValueAsString(layout);
        } catch (JsonProcessingException e) {
            // Return a simpler, smaller JSON if error
            return "{\"rows\":5,\"seatsPerRow\":20,\"premiumRows\":2}";
        }
    }
    
    @Transactional
    public void updateBookedSeats(Long showId, List<String> seatNumbers) {
        Show show = showRepository.findById(showId)
            .orElseThrow(() -> new RuntimeException("Show not found"));
        
        try {
            List<String> bookedSeats = objectMapper.readValue(show.getBookedSeats(), List.class);
            bookedSeats.addAll(seatNumbers);
            show.setBookedSeats(objectMapper.writeValueAsString(bookedSeats));
            show.setAvailableSeats(show.getAvailableSeats() - seatNumbers.size());
            showRepository.save(show);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error updating booked seats");
        }
    }
    
    public Long getTotalShows() {
        return showRepository.countActiveShows();
    }
    
    private ShowDTO convertToDTO(Show show) {
        ShowDTO dto = new ShowDTO();
        dto.setId(show.getId());
        dto.setShowDate(show.getShowDate());
        dto.setShowTime(show.getShowTime());
        dto.setEndTime(show.getEndTime());
        dto.setPriceClassic(show.getPriceClassic());
        dto.setPricePremium(show.getPricePremium());
        dto.setAvailableSeats(show.getAvailableSeats());
        dto.setBookedSeats(show.getBookedSeats());
        dto.setActive(show.isActive());
        
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
}