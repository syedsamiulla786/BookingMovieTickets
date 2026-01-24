package com.showtime.dto.request;

import lombok.Data;
import java.time.LocalDate;

@Data
public class MovieFilterRequest {
    private String city;
    private String language;
    private String genre;
    private LocalDate date;
    private String sortBy = "releaseDate";
    private String sortOrder = "desc";
    private Integer page = 0;
    private Integer size = 12;
}
