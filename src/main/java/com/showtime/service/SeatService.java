package com.showtime.service;

import com.showtime.dto.SeatDTO;
import com.showtime.model.*;
import com.showtime.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeatService {
    
    private final SeatRepository seatRepository;
    
    @Transactional
    public void createSeatsForShow(Show show, Screen screen) {
        List<Seat> seats = new ArrayList<>();
        int totalRows = 5;
        int seatsPerRow = screen.getTotalSeats() / totalRows;
        
        for (int row = 0; row < totalRows; row++) {
            for (int seatNum = 1; seatNum <= seatsPerRow; seatNum++) {
                String seatNumber = String.format("%c%d", 'A' + row, seatNum);
                
                Seat seat = new Seat();
                seat.setShow(show);
                seat.setSeatNumber(seatNumber);
                seat.setSeatRow(String.valueOf((char) ('A' + row)));
                seat.setSeatColumn(seatNum);
                
                // First 2 rows are premium
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
    
//    @Transactional
//    public void deleteSeatsForShow(Long showId) {
//        seatRepository.deleteByShowId(showId);
//    }
    
    @Transactional
    public void deleteSeatsForShow(Long showId) {
        List<Seat> seats = seatRepository.findByShowId(showId);
        seatRepository.deleteAll(seats);
    }
    
    public List<SeatDTO> getSeatsForShow(Long showId) {
        List<Seat> seats = seatRepository.findByShowId(showId);
        return seats.stream()
            .map(this::convertToDTO)
            .collect(java.util.stream.Collectors.toList());
    }
    
    private SeatDTO convertToDTO(Seat seat) {
        SeatDTO dto = new SeatDTO();
        dto.setId(seat.getId());
        dto.setSeatNumber(seat.getSeatNumber());
        dto.setSeatType(seat.getSeatType().name());
        dto.setPrice(seat.getPrice());
        dto.setStatus(seat.getStatus().name());
        dto.setIsAvailable(seat.getStatus() == Seat.SeatStatus.AVAILABLE);
        return dto;
    }
}