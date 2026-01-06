package com.showtime.controller;

import com.showtime.dto.*;
import com.showtime.dto.request.*;
import com.showtime.dto.response.*;
import com.showtime.service.PaymentService;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    
    private final PaymentService paymentService;
    
    @PostMapping("/initiate")
    public ResponseEntity<PaymentResponse> initiatePayment(@RequestBody PaymentRequest request) {
        return ResponseEntity.ok(paymentService.initiatePayment(request));
    }
    
    @PostMapping("/verify")
    public ResponseEntity<PaymentDTO> verifyPayment(@RequestBody PaymentVerificationRequest request) {
        return ResponseEntity.ok(paymentService.verifyPayment(request));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<PaymentDTO> getPaymentDetails(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getPaymentDetails(id));
    }
    
    @PostMapping("/{id}/refund")
    public ResponseEntity<PaymentDTO> processRefund(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        return ResponseEntity.ok(paymentService.processRefund(id, request.get("reason")));
    }
    
    // Add dummy payment simulation endpoint
    @PostMapping("/simulate-success/{bookingId}")
    public ResponseEntity<PaymentDTO> simulateSuccess(@PathVariable Long bookingId) {
        PaymentVerificationRequest dummyRequest = new PaymentVerificationRequest();
        dummyRequest.setRazorpayPaymentId("pay_dummy_" + System.currentTimeMillis());
        dummyRequest.setRazorpayOrderId("order_dummy_" + System.currentTimeMillis());
        dummyRequest.setRazorpaySignature("dummy_signature");
        
        return ResponseEntity.ok(paymentService.verifyPayment(dummyRequest));
    }
}