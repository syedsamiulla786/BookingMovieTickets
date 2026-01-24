package com.showtime.dto.request;

import lombok.Data;
import java.util.List;
import java.util.Map;

import com.showtime.dto.ScreenDTO;
import com.showtime.dto.SeatDTO;

@Data
public class SeatLayoutResponse {
    private Long showId;
    private List<List<SeatDTO>> seatLayout;
    private Map<String, Double> seatPrices; // {"CLASSIC": 200, "PREMIUM": 350}
    private List<String> bookedSeats;
    private List<String> lockedSeats;
    private Integer totalSeats;
    private Integer availableSeats;
    private Integer bookedSeatsCount;
    private ScreenDTO screen;
}
