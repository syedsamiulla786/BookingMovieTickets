package com.showtime.service;

import com.showtime.dto.response.PaymentResponse;
import com.showtime.model.Booking;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DummyPaymentService {
    
    public PaymentResponse createDummyOrder(Booking booking) {
        PaymentResponse response = new PaymentResponse();
        response.setPaymentId("pay_dummy_" + UUID.randomUUID().toString().substring(0, 8));
        response.setOrderId("order_dummy_" + UUID.randomUUID().toString().substring(0, 8));
        response.setAmount(booking.getTotalAmount());
        response.setCurrency("INR");
        response.setStatus("created");
        response.setRazorpayKey("dummy_key");
        response.setCallbackUrl("/payment-success");
        
        return response;
    }
    
    public boolean verifyDummyPayment(String orderId, String paymentId, String signature) {
        // Always return true for dummy payment
        return true;
    }
}