package com.showtime.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Booking {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "booking_reference")//, unique = true
    private String bookingReference;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "show_id", nullable = false)
    private Show show;
    
    @Size(min=2,max=20)
    @Column(name = "seat_numbers",length = 20, nullable = false)//length ->applies to varchar(20)
    private String seatNumbers; // JSON array
    
    @Column(name = "seat_type")
    private String seatType; // CLASSIC or PREMIUM
    
    @Column(name = "total_seats")
    private Integer totalSeats;
    
    @Column(name = "total_amount", nullable = false)
    private Double totalAmount;
    
    @Column(name = "convenience_fee")
    private Double convenienceFee = 30.0;
    
    @Column(name = "tax_amount")
    private Double taxAmount = 0.0;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "booking_status")
    private BookingStatus bookingStatus = BookingStatus.CONFIRMED;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status")
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;
    
    @Column(name = "booked_at", updatable = false)
    private LocalDateTime bookedAt = LocalDateTime.now();
    
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;
    
    @Column(name = "cancellation_reason")
    private String cancellationReason;
    
    @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Payment payment;
    
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.LAZY)// by default EAGER
    private List<Ticket> tickets = new ArrayList<>();
    
    public enum BookingStatus {
        CONFIRMED,CANCELLED,EXPIRED
    }
    
    public enum PaymentStatus {
        PENDING, COMPLETED, FAILED, REFUNDED
    }
}