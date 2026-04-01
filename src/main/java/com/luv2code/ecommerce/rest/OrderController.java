package com.luv2code.ecommerce.rest;

import com.luv2code.ecommerce.entity.Order;
import com.luv2code.ecommerce.entity.OrderItem;
import com.luv2code.ecommerce.dao.OrderRepository;
import com.luv2code.ecommerce.dao.OrderItemRepository;
import com.luv2code.ecommerce.dto.OrderRequest;
import com.luv2code.ecommerce.dto.OrderItemRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "http://localhost:4200")
public class OrderController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    /**
     * Create a new order
     * POST /api/orders/create
     * Body:
     * {
     *   "customerEmail": "customer@example.com",
     *   "totalPrice": 99.99,
     *   "totalQuantity": 2,
     *   "billingAddress": "123 Main St",
     *   "shippingAddress": "123 Main St",
     *   "orderItems": [
     *     {
     *       "productId": 1,
     *       "quantity": 1,
     *       "unitPrice": 49.99,
     *       "imageUrl": "..."
     *     }
     *   ]
     * }
     */
    @PostMapping("/create")
    public ResponseEntity<?> createOrder(@Valid @RequestBody OrderRequest orderRequest, BindingResult bindingResult) {
        try {
            // Check for validation errors
            if (bindingResult.hasErrors()) {
                String errors = bindingResult.getFieldErrors().stream()
                        .map(error -> error.getField() + ": " + error.getDefaultMessage())
                        .collect(Collectors.joining(", "));
                Map<String, String> errorMap = new HashMap<>();
                errorMap.put("error", "Validation failed: " + errors);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMap);
            }

            // Create order
            Order order = new Order();
            order.setOrderTrackingNumber(generateOrderTrackingNumber());
            order.setTotalPrice(orderRequest.getTotalPrice());
            order.setTotalQuantity(orderRequest.getTotalQuantity());
            order.setCustomerEmail(orderRequest.getCustomerEmail());
            order.setBillingAddress(orderRequest.getBillingAddress());
            order.setShippingAddress(orderRequest.getShippingAddress());
            order.setStatus("pending");

            // Save order
            Order savedOrder = orderRepository.save(order);

            // Create order items
            if (orderRequest.getOrderItems() != null && !orderRequest.getOrderItems().isEmpty()) {
                Set<OrderItem> orderItems = new HashSet<>();
                for (OrderItemRequest itemRequest : orderRequest.getOrderItems()) {
                    OrderItem orderItem = new OrderItem();
                    orderItem.setProductId(itemRequest.getProductId());
                    orderItem.setQuantity(itemRequest.getQuantity());
                    orderItem.setUnitPrice(itemRequest.getUnitPrice());
                    orderItem.setImageUrl(itemRequest.getImageUrl());
                    orderItem.setOrder(savedOrder);

                    OrderItem savedItem = orderItemRepository.save(orderItem);
                    orderItems.add(savedItem);
                }
                savedOrder.setOrderItems(orderItems);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("orderId", savedOrder.getId());
            response.put("orderTrackingNumber", savedOrder.getOrderTrackingNumber());
            response.put("totalPrice", savedOrder.getTotalPrice());
            response.put("totalQuantity", savedOrder.getTotalQuantity());
            response.put("status", savedOrder.getStatus());
            response.put("message", "Order created successfully");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to create order: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Get order by ID
     * GET /api/orders/{orderId}
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrder(@PathVariable Long orderId) {
        try {
            Optional<Order> order = orderRepository.findById(orderId);

            if (order.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("Order not found"));
            }

            return ResponseEntity.ok(order.get());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * Get all orders for a customer
     * GET /api/orders/customer/{email}
     */
    @GetMapping("/customer/{email}")
    public ResponseEntity<?> getOrdersByEmail(@PathVariable String email) {
        try {
            List<Order> customerOrders = orderRepository.findByCustomerEmail(email);

            if (customerOrders.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("No orders found for customer"));
            }

            return ResponseEntity.ok(customerOrders);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * Update order status
     * PUT /api/orders/{orderId}
     * Body: { "status": "completed" }
     */
    @PutMapping("/{orderId}")
    public ResponseEntity<?> updateOrder(@PathVariable Long orderId, @RequestBody Map<String, String> request) {
        try {
            Optional<Order> orderOpt = orderRepository.findById(orderId);

            if (orderOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("Order not found"));
            }

            Order order = orderOpt.get();
            String newStatus = request.get("status");

            if (newStatus != null && !newStatus.isEmpty()) {
                order.setStatus(newStatus);
                orderRepository.save(order);
            }

            return ResponseEntity.ok(order);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error: " + e.getMessage()));
        }
    }

    // Helper method to generate tracking number
    private String generateOrderTrackingNumber() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
