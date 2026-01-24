package com.showtime.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.showtime.model.*;
import com.showtime.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SeatInitializationService {
    
    private final SeatRepository seatRepository;
    private final ObjectMapper objectMapper;
    
    @Transactional
    public void initializeSeatsForShow(Show show, Screen screen) {
        List<Seat> seats = new ArrayList<>();
        
        try {
            // Parse seats layout from screen
            Map<String, Object> layout = objectMapper.readValue(
                screen.getSeatsLayout(), Map.class);
            
            int rows = (int) layout.get("rows");
            int seatsPerRow = (int) layout.get("seatsPerRow");
            int premiumRows = (int) layout.get("premiumRows");
            
            // Create seats for each row and column
            for (int row = 0; row < rows; row++) {
                char rowChar = (char) ('A' + row);
                boolean isPremium = row < premiumRows;
                
                for (int col = 1; col <= seatsPerRow; col++) {
                    String seatNumber = rowChar + String.valueOf(col);
                    
                    Seat seat = new Seat();
                    seat.setShow(show);
                    seat.setSeatNumber(seatNumber);
                    seat.setSeatRow(String.valueOf(rowChar));
                    seat.setSeatColumn(col);
                    seat.setSeatType(isPremium ? 
                        Seat.SeatType.PREMIUM : Seat.SeatType.CLASSIC);
                    seat.setPrice(isPremium ? 
                        show.getPricePremium() : show.getPriceClassic());
                    seat.setStatus(Seat.SeatStatus.AVAILABLE);
                    
                    seats.add(seat);
                }
            }
            
            // Save all seats
            seatRepository.saveAll(seats);
            
            // Update show's available seats count
            show.setAvailableSeats(seats.size());
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize seats: " + e.getMessage(), e);
        }
    }
}