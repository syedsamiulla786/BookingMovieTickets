package com.showtime.service;

import com.showtime.dto.*;
import com.showtime.model.*;
import com.showtime.model.Booking.BookingStatus;
import com.showtime.repository.*;
import com.showtime.service.EmailService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import com.showtime.dto.response.*;
import com.showtime.exception.ResourceNotFoundException;
import com.showtime.exception.ValidationException;
import com.showtime.dto.request.*;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {
    
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ShowRepository showRepository;
    private final PaymentRepository paymentRepository;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final SeatRepository seatRepository;
    
    @Transactional
    public BookingResponse createBooking(BookingRequest request, User user) {
        // Get show
        Show show = showRepository.findById(request.getShowId())
            .orElseThrow(() -> new RuntimeException("Show not found"));
        
        // Get seats
        List<Seat> seats = seatRepository.findByShowIdAndSeatNumberIn(
            request.getShowId(), request.getSeatNumbers());
        
        // Validate seat availability
        validateSeats(seats, request.getSeatNumbers());
        
        // Calculate total
        double totalAmount = calculateTotal(seats);
        double convenienceFee = 30.0 * seats.size();
        double taxAmount = totalAmount * 0.18;
        double finalAmount = totalAmount + convenienceFee + taxAmount;
        
        // Create booking
        String bookingRef = "BKG" + System.currentTimeMillis() + user.getId();
        
        Booking booking = new Booking();
        booking.setBookingReference(bookingRef);
        booking.setUser(user);
        booking.setShow(show);
        booking.setTotalSeats(seats.size());
        booking.setTotalAmount(finalAmount);
        booking.setConvenienceFee(convenienceFee);
        booking.setTaxAmount(taxAmount);
        booking.setBookingStatus(Booking.BookingStatus.CONFIRMED);
        booking.setPaymentStatus(Booking.PaymentStatus.PENDING);
        
        // Store seat numbers as JSON
        booking.setSeatNumbers(convertToJson(request.getSeatNumbers()));
        
        Booking savedBooking = bookingRepository.save(booking);
        
        // Update seat status
        updateSeatStatus(seats, savedBooking, Seat.SeatStatus.BOOKED);
        
        // Create payment
        createPayment(savedBooking, request.getPaymentMethod(), finalAmount);
        
        // Generate tickets
        List<TicketDTO> tickets = generateTickets(savedBooking, seats);
        
        return new BookingResponse(
            savedBooking.getId(),
            bookingRef,
            finalAmount,
            "PENDING",
            tickets
        );
    }
    
    private void validateSeats(List<Seat> seats, List<String> requestedSeats) {
        if (seats.size() != requestedSeats.size()) {
            throw new RuntimeException("Some seats are not available");
        }
        
        for (Seat seat : seats) {
            if (seat.getStatus() != Seat.SeatStatus.AVAILABLE) {
                throw new RuntimeException("Seat " + seat.getSeatNumber() + " is not available");
            }
        }
    }
    
    private double calculateTotal(List<Seat> seats) {
        return seats.stream()
            .mapToDouble(Seat::getPrice)
            .sum();
    }
    
    private void updateSeatStatus(List<Seat> seats, Booking booking, Seat.SeatStatus status) {
        seats.forEach(seat -> {
            seat.setStatus(status);
            seat.setBooking(booking);
        });
        seatRepository.saveAll(seats);
    }
    
    private void createPayment(Booking booking, String paymentMethod, double amount) {
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setPaymentMethod(paymentMethod);
        payment.setAmount(amount);
        payment.setStatus(Payment.PaymentStatus.PENDING);
        paymentRepository.save(payment);
    }
    
    private List<TicketDTO> generateTickets(Booking booking, List<Seat> seats) {
        return seats.stream()
            .map(seat -> createTicketDTO(booking, seat))
            .collect(Collectors.toList());
    }
    
    private TicketDTO createTicketDTO(Booking booking, Seat seat) {
        TicketDTO ticket = new TicketDTO();
        ticket.setTicketNumber("TKT" + System.currentTimeMillis() + seat.getSeatNumber());
        ticket.setSeatNumber(seat.getSeatNumber());
        ticket.setSeatType(seat.getSeatType().name());
        ticket.setPrice(seat.getPrice());
        ticket.setQrCodeUrl("/api/tickets/" + booking.getBookingReference() + "/qr");
        ticket.setIsUsed(false);
        return ticket;
    }
    
    
    
//    private void updateShowBookedSeats(Show show, List<String> seatNumbers) {
//        try {
//            List<String> bookedSeats = objectMapper.readValue(show.getBookedSeats(), List.class);
//            bookedSeats.addAll(seatNumbers);
//            show.setBookedSeats(objectMapper.writeValueAsString(bookedSeats));
//            show.setAvailableSeats(show.getAvailableSeats() - seatNumbers.size());
//            showRepository.save(show);
//        } catch (JsonProcessingException e) {
//            throw new RuntimeException("Error updating show seats");
//        }
//    }
    
    private String convertToJson(List<String> seatNumbers) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(seatNumbers);
        } catch (Exception e) {
            return "[]";
        }
    }
    
    private void generateTickets(Booking booking, List<String> seatNumbers, double seatPrice) {
        for (String seatNumber : seatNumbers) {
            Ticket ticket = new Ticket();
            ticket.setBooking(booking);
            ticket.setTicketNumber("TKT" + System.currentTimeMillis() + seatNumber);
            ticket.setSeatNumber(seatNumber);
            ticket.setSeatType(booking.getSeatType());
            ticket.setPrice(seatPrice);
            // Generate QR code URL (in real app, generate actual QR code)
            ticket.setQrCodeUrl("/api/bookings/" + booking.getId() + "/ticket/" + ticket.getTicketNumber() + "/qr");
        }
    }
    
    public List<BookingDTO> getUserBookings(String userId) {
    	Optional<User> user = userRepository.findByEmail(userId);
        return bookingRepository.findByUserIdOrderByBookedAtDesc(user.get().getId()).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    public BookingHistoryDTO getUserBookingHistory(Long userId) {
        BookingHistoryDTO history = new BookingHistoryDTO();
        history.setUpcomingBookings(bookingRepository.findUpcomingBookings(userId).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList()));
        history.setPastBookings(bookingRepository.findPastBookings(userId).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList()));
        history.setCancelledBookings(bookingRepository.findByUserIdAndBookingStatusOrderByBookedAtDesc(
            userId, "CANCELLED").stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList()));
        return history;
    }
    
    @Transactional
    public void cancelBooking(Long bookingId, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Booking not found"));
        
        if (!booking.getBookingStatus().equals(Booking.BookingStatus.CONFIRMED)) {
            throw new RuntimeException("Booking cannot be cancelled");
        }
        
        // Update booking status
        booking.setBookingStatus(Booking.BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        booking.setCancellationReason(reason);
        booking.setPaymentStatus(Booking.PaymentStatus.REFUNDED);
        bookingRepository.save(booking);
        
        // Update payment status
        Optional<Payment> paymentOpt = paymentRepository.findById(booking.getPayment().getId());
        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();
            payment.setStatus(Payment.PaymentStatus.REFUNDED);
            payment.setRefundDate(LocalDateTime.now());
            payment.setRefundAmount(booking.getTotalAmount());
            payment.setRefundReason(reason);
            paymentRepository.save(payment);
        }
        
        // Update show available seats
        try {
            Show show = booking.getShow();
            List<String> bookedSeats = objectMapper.readValue(show.getBookedSeats(), List.class);
            List<String> cancelledSeats = objectMapper.readValue(booking.getSeatNumbers(), List.class);
            bookedSeats.removeAll(cancelledSeats);
            show.setBookedSeats(objectMapper.writeValueAsString(bookedSeats));
            show.setAvailableSeats(show.getAvailableSeats() + cancelledSeats.size());
            showRepository.save(show);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error updating show seats");
        }
        
        // Send cancellation email
        emailService.sendBookingCancellation(booking.getUser().getEmail(), booking);
    }
    
    public Long getTotalBookings() {
        return bookingRepository.countConfirmedBookings();
    }
    
    public Double getTotalRevenue() {
        return bookingRepository.getTotalRevenue();
    }
    
    public BookingDTO convertToDTO(Booking booking) {
        BookingDTO dto = new BookingDTO();
        dto.setId(booking.getId());
        dto.setBookingReference(booking.getBookingReference());
        dto.setTotalSeats(booking.getTotalSeats());
        dto.setTotalAmount(booking.getTotalAmount());
        dto.setConvenienceFee(booking.getConvenienceFee());
        dto.setTaxAmount(booking.getTaxAmount());
        dto.setBookingStatus(booking.getBookingStatus().name());
        dto.setPaymentStatus(booking.getPaymentStatus().name());
        dto.setBookedAt(booking.getBookedAt());
        
        try {
            dto.setSeatNumbers(objectMapper.readValue(booking.getSeatNumbers(), List.class));
        } catch (JsonProcessingException e) {
            dto.setSeatNumbers(new ArrayList<>());
        }
        
        if (booking.getShow() != null) {
            ShowDTO showDTO = new ShowDTO();
            showDTO.setId(booking.getShow().getId());
            showDTO.setShowDate(booking.getShow().getShowDate());
            showDTO.setShowTime(booking.getShow().getShowTime());
            
            if (booking.getShow().getMovie() != null) {
                MovieDTO movieDTO = new MovieDTO();
                movieDTO.setId(booking.getShow().getMovie().getId());
                movieDTO.setTitle(booking.getShow().getMovie().getTitle());
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
        
        return dto;
    }
    
     public List<BookingDTO> getAllBookings(int page, int size) {
    	 Sort s = Sort.by("bookedAt").descending();
         Pageable pageable = (Pageable) PageRequest.of(page, size,s);
         Page<Booking> bookings = bookingRepository.findAll(pageable);
         
         return bookings.stream()
             .map(this::convertToDTO)
             .collect(Collectors.toList());
     }
     
     public void updateBookingStatus(Long bookingId, String status) {
         Booking booking = bookingRepository.findById(bookingId)
             .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));
         
         // Validate status transition
         validateStatusTransition(booking.getBookingStatus(), status);
         
         // Update status
         booking.setBookingStatus(Booking.BookingStatus.valueOf(status.toUpperCase()));
         
         // If cancelling, update payment status too
         if (status.equalsIgnoreCase("CANCELLED")) {
             booking.setPaymentStatus(Booking.PaymentStatus.REFUNDED);
             booking.setCancelledAt(LocalDateTime.now());
             
             // Update available seats
             updateAvailableSeatsOnCancellation(booking);
             
             // Send cancellation notification
             notificationService.sendNotification(
                 booking.getUser().getId(),
                 "Booking Cancelled",
                 "Your booking " + booking.getBookingReference() + " has been cancelled by admin",
                 "booking_cancelled"
             );
         }
         
         bookingRepository.save(booking);
     }
     
     private void validateStatusTransition(BookingStatus bookingStatus, String newStatus) {
         Map<String, List<String>> allowedTransitions = Map.of(
             "CONFIRMED", List.of("CANCELLED", "EXPIRED"),
             "PENDING", List.of("CONFIRMED", "CANCELLED"),
             "CANCELLED", List.of(), // Cannot change from cancelled
             "EXPIRED", List.of()    // Cannot change from expired
         );
         
         List<String> allowed = allowedTransitions.get(bookingStatus);
         if (allowed == null || !allowed.contains(newStatus.toUpperCase())) {
             throw new ValidationException(
                 "Invalid status transition from " + bookingStatus + " to " + newStatus
             );
         }
     }
     
     private List<String> parseSeatNumbers(String seatNumbersJson) {
    	    try {
    	        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
    	        return mapper.readValue(seatNumbersJson, 
    	            mapper.getTypeFactory().constructCollectionType(List.class, String.class));
    	    } catch (Exception e) {
    	        return new java.util.ArrayList<>();
    	    }
    	}

//    	private ShowDTO convertShowToDTO(Show show) {
//    	    ShowDTO dto = new ShowDTO();
//    	    dto.setId(show.getId());
//    	    dto.setShowDate(show.getShowDate());
//    	    dto.setShowTime(show.getShowTime());
//    	    
//    	    if (show.getMovie() != null) {
//    	        dto.setMovie(convertMovieToDTO(show.getMovie()));
//    	    }
//    	    
//    	    if (show.getTheater() != null) {
//    	        dto.setTheater(convertTheaterToDTO(show.getTheater()));
//    	    }
//    	    
//    	    return dto;
//    	}
     
     private void updateAvailableSeatsOnCancellation(Booking booking) {
         try {
             Show show = booking.getShow();
             List<String> cancelledSeats = objectMapper.readValue(booking.getSeatNumbers(), List.class);
             List<String> bookedSeats = objectMapper.readValue(show.getBookedSeats(), List.class);
             
             // Remove cancelled seats from booked seats
             bookedSeats.removeAll(cancelledSeats);
             
             // Update show
             show.setBookedSeats(objectMapper.writeValueAsString(bookedSeats));
             show.setAvailableSeats(show.getAvailableSeats() + cancelledSeats.size());
             showRepository.save(show);
             
         } catch (JsonProcessingException e) {
             throw new RuntimeException("Error updating seats on cancellation");
         }
     }
     
     public Long getTodaysBookingsCount() {
         LocalDate today = LocalDate.now();
         return bookingRepository.countByBookedAtBetween(
             today.atStartOfDay(),
             today.atTime(LocalTime.MAX)
         );
     }
     
     public List<Object[]> getMonthlyRevenue() {
         LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
         return bookingRepository.getMonthlyRevenue(sixMonthsAgo);
     }
     
     public Page<BookingDTO> getAllBookings(Pageable pageable) {
         Page<Booking> bookings = bookingRepository.findAll(pageable);
         return bookings.map(this::convertToDTO);
     }
    
    
}