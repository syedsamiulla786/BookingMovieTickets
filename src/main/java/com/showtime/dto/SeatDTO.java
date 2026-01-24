package com.showtime.dto;

import lombok.Data;

@Data
public class SeatDTO {
    private Long id;
    private String seatNumber;
    private String seatType;
    private Double price;
    private String status;
    private Boolean isSelected = false;
    private Boolean isAvailable;
    
    // Position for frontend
    private Integer row;
    private Integer column;
    private String displayName;
    
    // Helper methods
    public void setAvailable(boolean available) {
        this.isAvailable = available;
    }
    
    public void setSelected(boolean selected) {
        this.isSelected = selected;
    }
    
    public boolean getAvailable() {
        return isAvailable != null ? isAvailable : false;
    }
    
    public boolean getSelected() {
        return isSelected != null ? isSelected : false;
    }
    
    // Constructor for easier creation
    public SeatDTO() {}
    
    public SeatDTO(String seatNumber, String seatType, Double price, boolean isAvailable) {
        this.seatNumber = seatNumber;
        this.seatType = seatType;
        this.price = price;
        this.isAvailable = isAvailable;
        this.status = isAvailable ? "AVAILABLE" : "BOOKED";
    }
}



/*
 * package com.showtime.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class SeatDTO {
    @Getter @Setter
    private Long id;
    
    @Getter @Setter
    private String seatNumber;
    
    @Getter @Setter
    private String seatType;
    
    @Getter @Setter
    private Double price;
    
    @Getter @Setter
    private String status;
    
    @Getter @Setter
    private Boolean isSelected = false;
    
    @Getter @Setter
    private Boolean isAvailable;
    
    @Getter @Setter
    private Integer row;
    
    @Getter @Setter
    private Integer column;
    
    @Getter @Setter
    private String displayName;
}
 * 
 * 
 */