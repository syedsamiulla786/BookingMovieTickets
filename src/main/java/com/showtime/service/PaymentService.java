//package com.showtime.service;
//
//import com.showtime.dto.*;
//import com.showtime.dto.request.*;
//import com.showtime.dto.response.*;
//import com.showtime.model.Booking;
//import com.showtime.model.Payment;
//import com.showtime.repository.BookingRepository;
//import com.showtime.repository.PaymentRepository;
//import com.showtime.service.EmailService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDateTime;
//import java.util.Map;
//
//@Service
//@RequiredArgsConstructor
//public class PaymentService {
//    
//    private final PaymentRepository paymentRepository;
//    private final BookingRepository bookingRepository;
//    private final EmailService emailService;
//    private final DummyPaymentService dummyPaymentService;
//    
//    @Transactional
//    public PaymentResponse initiatePayment(PaymentRequest request) {
//        Booking booking = bookingRepository.findById(request.getBookingId())
//            .orElseThrow(() -> new RuntimeException("Booking not found"));
//        
//        // Create dummy payment response
//        PaymentResponse response = dummyPaymentService.createDummyOrder(booking);
//        
//        // Create payment record
//        Payment payment = new Payment();
//        payment.setBooking(booking);
//        payment.setPaymentMethod(request.getPaymentMethod());
//        payment.setPaymentGateway("DUMMY");
//        payment.setGatewayTransactionId(response.getOrderId());
//        payment.setTransactionId(response.getPaymentId());
//        payment.setAmount(booking.getTotalAmount());
//        payment.setStatus(Payment.PaymentStatus.PENDING);
//        paymentRepository.save(payment);
//        
//        return response;
//    }
//    
//    @Transactional
//    public PaymentDTO verifyPayment(PaymentVerificationRequest request) {
//        // Always verify successfully for dummy payment
//        Payment payment = paymentRepository.findByGatewayTransactionId(request.getRazorpayOrderId())
//            .orElseThrow(() -> new RuntimeException("Payment not found"));
//        
//        // Mark as success
//        payment.setStatus(Payment.PaymentStatus.SUCCESS);
//        payment.setGatewayTransactionId(request.getRazorpayPaymentId());
//        payment.setTransactionId(request.getRazorpayPaymentId());
//        payment.setPaymentDate(LocalDateTime.now());
//        paymentRepository.save(payment);
//        
//        // Update booking
//        Booking booking = payment.getBooking();
//        booking.setPaymentStatus(Booking.PaymentStatus.COMPLETED);
//        bookingRepository.save(booking);
//        
//        // Send email
//        emailService.sendPaymentConfirmation(booking.getUser().getEmail(), booking, payment);
//        
//        return convertToDTO(payment);
//    }
//    
//    // ADD THIS METHOD
//    public PaymentDTO getPaymentDetails(Long id) {
//        Payment payment = paymentRepository.findById(id)
//            .orElseThrow(() -> new RuntimeException("Payment not found"));
//        return convertToDTO(payment);
//    }
//    
//    @Transactional
//    public PaymentDTO processRefund(Long paymentId, String reason) {
//        Payment payment = paymentRepository.findById(paymentId)
//            .orElseThrow(() -> new RuntimeException("Payment not found"));
//        
//        // Mark as refunded
//        payment.setStatus(Payment.PaymentStatus.REFUNDED);
//        payment.setRefundDate(LocalDateTime.now());
//        payment.setRefundAmount(payment.getAmount());
//        payment.setRefundReason(reason);
//        paymentRepository.save(payment);
//        
//        // Update booking
//        Booking booking = payment.getBooking();
//        booking.setPaymentStatus(Booking.PaymentStatus.REFUNDED);
//        bookingRepository.save(booking);
//        
//        return convertToDTO(payment);
//    }
//    
//    private PaymentDTO convertToDTO(Payment payment) {
//        PaymentDTO dto = new PaymentDTO();
//        dto.setId(payment.getId());
//        dto.setPaymentMethod(payment.getPaymentMethod());
//        dto.setPaymentGateway(payment.getPaymentGateway());
//        dto.setTransactionId(payment.getTransactionId());
//        dto.setAmount(payment.getAmount());
//        dto.setStatus(payment.getStatus().name());
//        dto.setBookingReference(payment.getBooking().getBookingReference());
//        return dto;
//    }
//}