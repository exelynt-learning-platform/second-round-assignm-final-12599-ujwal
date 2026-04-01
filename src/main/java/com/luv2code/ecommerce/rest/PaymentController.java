package com.luv2code.ecommerce.rest;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.luv2code.ecommerce.entity.Order;
import com.luv2code.ecommerce.dao.OrderRepository;
import com.luv2code.ecommerce.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "http://localhost:4200")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private OrderRepository orderRepository;

    /**
     * Create a payment intent for an order
     * Request: POST /api/payments/create-payment-intent
     * Body: { "orderId": 1 }
     */
    @PostMapping("/create-payment-intent")
    public ResponseEntity<?> createPaymentIntent(@RequestBody Map<String, Long> request) {
        try {
            Long orderId = request.get("orderId");

            // Retrieve order from database
            Order order = orderRepository.findById(orderId).orElse(null);

            if (order == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("Order not found"));
            }

            // Generate order tracking number if not present
            if (order.getOrderTrackingNumber() == null || order.getOrderTrackingNumber().isEmpty()) {
                order.setOrderTrackingNumber(generateOrderTrackingNumber());
                orderRepository.save(order);
            }

            // Create payment intent
            String clientSecret = paymentService.createPaymentIntentForOrder(order);

            Map<String, String> response = new HashMap<>();
            response.put("clientSecret", clientSecret);
            response.put("paymentIntentId", order.getStripePaymentIntentId());

            return ResponseEntity.ok(response);

        } catch (StripeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Stripe error: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * Get payment intent status
     * Request: GET /api/payments/payment-intent/{paymentIntentId}
     */
    @GetMapping("/payment-intent/{paymentIntentId}")
    public ResponseEntity<?> getPaymentIntent(@PathVariable String paymentIntentId) {
        try {
            PaymentIntent paymentIntent = paymentService.retrievePaymentIntent(paymentIntentId);

            Map<String, Object> response = new HashMap<>();
            response.put("id", paymentIntent.getId());
            response.put("status", paymentIntent.getStatus());
            response.put("amount", paymentIntent.getAmount());
            response.put("currency", paymentIntent.getCurrency());

            return ResponseEntity.ok(response);

        } catch (StripeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Stripe error: " + e.getMessage()));
        }
    }

    /**
     * Update order status based on payment intent
     * Request: POST /api/payments/update-order-status
     * Body: { "paymentIntentId": "pi_..." }
     */
    @PostMapping("/update-order-status")
    public ResponseEntity<?> updateOrderStatus(@RequestBody Map<String, String> request) {
        try {
            String paymentIntentId = request.get("paymentIntentId");

            boolean success = paymentService.updateOrderStatus(paymentIntentId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", success ? "Order updated successfully" : "Order not found");

            return ResponseEntity.ok(response);

        } catch (StripeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Stripe error: " + e.getMessage()));
        }
    }

    /**
     * Cancel a payment intent
     * Request: POST /api/payments/cancel/{paymentIntentId}
     */
    @PostMapping("/cancel/{paymentIntentId}")
    public ResponseEntity<?> cancelPayment(@PathVariable String paymentIntentId) {
        try {
            PaymentIntent canceledIntent = paymentService.cancelPaymentIntent(paymentIntentId);

            Map<String, Object> response = new HashMap<>();
            response.put("id", canceledIntent.getId());
            response.put("status", canceledIntent.getStatus());
            response.put("message", "Payment canceled successfully");

            return ResponseEntity.ok(response);

        } catch (StripeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Stripe error: " + e.getMessage()));
        }
    }

    /**
     * Get Stripe publishable key
     * Request: GET /api/payments/config
     */
    @GetMapping("/config")
    public ResponseEntity<?> getConfig() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Stripe config endpoint. Use publishable key from frontend environment.");
        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "OK");
        return ResponseEntity.ok(response);
    }

    // Helper method to generate order tracking number
    private String generateOrderTrackingNumber() {
        return "ORD-" + System.currentTimeMillis();
    }

    // Inner class for error response
    public static class ErrorResponse {
        public String error;

        public ErrorResponse(String error) {
            this.error = error;
        }
    }
}
