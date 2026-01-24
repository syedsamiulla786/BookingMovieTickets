package com.showtime.dto.request;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ShowRequest {
    private Long movieId;
    private Long theaterId;
    private Integer screenNumber;
    private LocalDate showDate;
    private LocalTime showTime;
    private Double priceClassic;
    private Double pricePremium;
}
