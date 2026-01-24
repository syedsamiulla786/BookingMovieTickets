package com.showtime.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "seats", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"show_id", "seat_number"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Seat {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "show_id", nullable = false)
    private Show show;
    
    @Column(name = "seat_number", nullable = false)
    private String seatNumber; // Format: "A1", "A2", "B1", etc.
    
    @Enumerated(EnumType.STRING)
    @Column(name = "seat_type", nullable = false)
    private SeatType seatType;
    
    @Column(name = "seat_row") // Changed from row_number to seat_row
    private String seatRow; // A, B, C, etc.
    
    @Column(name = "seat_column") // Changed from column_number to seat_column
    private Integer seatColumn; // 1, 2, 3, etc.
    
    @Column(name = "price", nullable = false)
    private Double price;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SeatStatus status = SeatStatus.AVAILABLE;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;
    
    @Column(name = "locked_until")
    private java.time.LocalDateTime lockedUntil;
    
    @Column(name = "locked_by")
    private Long lockedBy; // User ID who locked the seat
    
    public enum SeatType {
        CLASSIC, PREMIUM, RECLINER, COUPLE
    }
    
    public enum SeatStatus {
        AVAILABLE, BOOKED, LOCKED, MAINTENANCE
    }
    
    // This method runs automatically before the entity is saved to the database
    @PrePersist
    @PreUpdate
    public void prePersist() {
        // Auto-populate seatRow from seatNumber (first character)
        if (this.seatRow == null && this.seatNumber != null && !this.seatNumber.isEmpty()) {
            this.seatRow = String.valueOf(this.seatNumber.charAt(0));
        }
        
        // Auto-populate seatColumn from seatNumber (remaining characters)
        if (this.seatColumn == null && this.seatNumber != null && this.seatNumber.length() > 1) {
            try {
                // Extract numbers from seatNumber (e.g., "A1" -> 1, "B12" -> 12)
                String numberPart = this.seatNumber.substring(1);
                this.seatColumn = Integer.parseInt(numberPart);
            } catch (NumberFormatException e) {
                this.seatColumn = 0; // Default value if parsing fails
            }
        }
    }
}