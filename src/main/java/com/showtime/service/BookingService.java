package com.showtime.service;

import com.showtime.dto.*;
import com.showtime.dto.request.BookingRequest;
import com.showtime.dto.response.BookingResponse;
import com.showtime.model.*;
import com.showtime.repository.*;
//import com.showtime.util.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {
    
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ShowRepository showRepository;
    private final SeatRepository seatRepository;
    private final PaymentRepository paymentRepository;
    private final EmailService emailService;
    
    // Create booking
    @Transactional
    public BookingResponse createBooking(BookingRequest request, User user) {
        // Get show
        Show show = showRepository.findById(request.getShowId())
            .orElseThrow(() -> new RuntimeException("Show not found"));
        
        // Get seats
        List<Seat> seats = seatRepository.findByShowIdAndSeatNumberIn(
            request.getShowId(), request.getSeatNumbers());
        
        // Validate seat availability
        validateSeatAvailability(seats, request.getSeatNumbers());
        
        // Calculate total amount
        double seatTotal = seats.stream()
            .mapToDouble(Seat::getPrice)
            .sum();
        double convenienceFee = 30.0 * seats.size();
        double taxAmount = seatTotal * 0.18;
        double totalAmount = seatTotal + convenienceFee + taxAmount;
        
        // Generate booking reference
        String bookingReference = "BKG" + System.currentTimeMillis() + user.getId();
        
        // Create booking
        Booking booking = new Booking();
        booking.setBookingReference(bookingReference);
        booking.setUser(user);
        booking.setShow(show);
        booking.setSeatNumbers(String.join(",", request.getSeatNumbers()));
        booking.setSeatType(request.getSeatType());
        booking.setTotalSeats(seats.size());
        booking.setTotalAmount(totalAmount);
        booking.setConvenienceFee(convenienceFee);
        booking.setTaxAmount(taxAmount);
        booking.setBookingStatus(Booking.BookingStatus.CONFIRMED);
        booking.setPaymentStatus(Booking.PaymentStatus.PENDING);
        booking.setBookedAt(LocalDateTime.now());
        
        Booking savedBooking = bookingRepository.save(booking);
        
        // Update seat status to BOOKED
        seats.forEach(seat -> {
            seat.setStatus(Seat.SeatStatus.BOOKED);
            seat.setBooking(savedBooking);
        });
        seatRepository.saveAll(seats);
        
        // Create payment record
        createPayment(savedBooking, request.getPaymentMethod(), totalAmount);
        
        // Generate tickets
        List<TicketDTO> tickets = generateTickets(savedBooking, seats);
        
        // Send confirmation email
        emailService.sendBookingConfirmation(user.getEmail(), savedBooking);
        
        return new BookingResponse(
            savedBooking.getId(),
            bookingReference,
            totalAmount,
            "PENDING",
            tickets
        );
    }
    
    // Validate seat availability
    private void validateSeatAvailability(List<Seat> seats, List<String> requestedSeats) {
        if (seats.size() != requestedSeats.size()) {
            throw new RuntimeException("Some seats are not available");
        }
        
        for (Seat seat : seats) {
            if (seat.getStatus() != Seat.SeatStatus.AVAILABLE) {
                throw new RuntimeException("Seat " + seat.getSeatNumber() + " is not available");
            }
        }
    }
    
    // Create payment record
    private void createPayment(Booking booking, String paymentMethod, double amount) {
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setPaymentMethod(paymentMethod);
        payment.setAmount(amount);
        payment.setStatus(Payment.PaymentStatus.PENDING);
        paymentRepository.save(payment);
    }
    
    // Generate tickets
    private List<TicketDTO> generateTickets(Booking booking, List<Seat> seats) {
        List<TicketDTO> tickets = new ArrayList<>();
        
        for (Seat seat : seats) {
            TicketDTO ticket = new TicketDTO();
            ticket.setTicketNumber("TKT" + UUID.randomUUID().toString().substring(0, 8));
            ticket.setSeatNumber(seat.getSeatNumber());
            ticket.setSeatType(seat.getSeatType().name());
            ticket.setPrice(seat.getPrice());
            ticket.setQrCodeUrl("/api/tickets/" + booking.getBookingReference() + "/" + seat.getSeatNumber());
            ticket.setIsUsed(false);
            ticket.setCreatedAt(LocalDateTime.now());
            
            tickets.add(ticket);
        }
        
        return tickets;
    }
    
    // Get user bookings
    public List<BookingDTO> getUserBookings(String username) {
    	System.out.println("User "+username);
        User user = userRepository.findByEmail(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        return bookingRepository.findByUserIdOrderByBookedAtDesc(user.getId()).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    // Get booking by ID
    public BookingDTO getBookingById(Long id, User user) {
        Booking booking = bookingRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Booking not found"));
        
        // Check if user owns the booking or is admin
        if (!booking.getUser().getId().equals(user.getId()) && 
            user.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("Access denied");
        }
        
        return convertToDTO(booking);
    }
    
    // Cancel booking
    @Transactional
    public void cancelBooking(Long bookingId, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Booking not found"));
        
        if (booking.getBookingStatus() != Booking.BookingStatus.CONFIRMED) {
            throw new RuntimeException("Only confirmed bookings can be cancelled");
        }
        
        // Update booking status
        booking.setBookingStatus(Booking.BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        booking.setCancellationReason(reason);
        booking.setPaymentStatus(Booking.PaymentStatus.REFUNDED);
        
        // Releasing seats
        List<Seat> seats = seatRepository.findByBookingId(bookingId);
        seats.forEach(seat -> {
            seat.setStatus(Seat.SeatStatus.AVAILABLE);
            seat.setBooking(null);
        });
        seatRepository.saveAll(seats);
        
        // Update payment status
        Payment payment = paymentRepository.findByBookingId(bookingId)
            .orElseThrow(() -> new RuntimeException("Payment not found"));
        payment.setStatus(Payment.PaymentStatus.REFUNDED);
        payment.setRefundDate(LocalDateTime.now());
        payment.setRefundAmount(booking.getTotalAmount());
        payment.setRefundReason(reason);
        paymentRepository.save(payment);
        
        bookingRepository.save(booking);
        
        // Send cancellation email
        emailService.sendBookingCancellation(booking.getUser().getEmail(), booking);
    }
    
    // Get booking history
    public BookingHistoryDTO getUserBookingHistory(Long userId) {
        BookingHistoryDTO history = new BookingHistoryDTO();
        
        history.setUpcomingBookings(
            bookingRepository.findUpcomingBookings(userId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList())
        );
        
        history.setPastBookings(
            bookingRepository.findPastBookings(userId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList())
        );
        
        history.setCancelledBookings(
            bookingRepository.findByUserIdAndBookingStatusOrderByBookedAtDesc(
                userId, "CANCELLED").stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList())
        );
        
        return history;
    }
    
    // Convert Booking to BookingDTO
    private BookingDTO convertToDTO(Booking booking) {
        BookingDTO dto = new BookingDTO();
        dto.setId(booking.getId());
        dto.setBookingReference(booking.getBookingReference());
        
        // Parsing seat numbers
        List<String> seatNumbers = new ArrayList<>();
        if (booking.getSeatNumbers() != null) {
            String[] seats = booking.getSeatNumbers().split(",");
            for (String seat : seats) {
                if (!seat.trim().isEmpty()) {
                    seatNumbers.add(seat.trim());
                }
            }
        }
        dto.setSeatNumbers(seatNumbers);
        
        dto.setSeatType(booking.getSeatType());
        dto.setTotalSeats(booking.getTotalSeats());
        dto.setTotalAmount(booking.getTotalAmount());
        dto.setConvenienceFee(booking.getConvenienceFee());
        dto.setTaxAmount(booking.getTaxAmount());
        dto.setBookingStatus(booking.getBookingStatus().name());
        dto.setPaymentStatus(booking.getPaymentStatus().name());
        dto.setBookedAt(booking.getBookedAt());
        
        // Convert Show to ShowDTO
        if (booking.getShow() != null) {
            ShowDTO showDTO = new ShowDTO();
            showDTO.setId(booking.getShow().getId());
            showDTO.setShowDate(booking.getShow().getShowDate());
            showDTO.setShowTime(booking.getShow().getShowTime());
            showDTO.setEndTime(booking.getShow().getEndTime());
            
            if (booking.getShow().getMovie() != null) {
                MovieDTO movieDTO = new MovieDTO();
                movieDTO.setId(booking.getShow().getMovie().getId());
                movieDTO.setTitle(booking.getShow().getMovie().getTitle());
                movieDTO.setDuration(booking.getShow().getMovie().getDuration());
                showDTO.setMovie(movieDTO);
            }
            
            if (booking.getShow().getTheater() != null) {
                TheaterDTO theaterDTO = new TheaterDTO();
                theaterDTO.setId(booking.getShow().getTheater().getId());
                theaterDTO.setName(booking.getShow().getTheater().getName());
                theaterDTO.setCity(booking.getShow().getTheater().getCity());
                showDTO.setTheater(theaterDTO);
            }
            
            dto.setShow(showDTO);
        }
        
        // Get payment details
        Payment payment = paymentRepository.findByBookingId(booking.getId()).orElse(null);
        if (payment != null) {
            PaymentDTO paymentDTO = new PaymentDTO();
            paymentDTO.setId(payment.getId());
            paymentDTO.setPaymentMethod(payment.getPaymentMethod());
            paymentDTO.setAmount(payment.getAmount());
            paymentDTO.setStatus(payment.getStatus().name());
//            paymentDTO.setPaymentDate(payment.getPaymentDate());
            dto.setPayment(paymentDTO);
        }
        
        return dto;
    }
    
    // Get statistics
    public Long getTotalBookings() {
        return bookingRepository.countConfirmedBookings();
    }
    
    public Double getTotalRevenue() {
        return bookingRepository.getTotalRevenue();
    }
}