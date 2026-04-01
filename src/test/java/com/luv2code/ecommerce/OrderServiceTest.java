package com.luv2code.ecommerce;

import com.luv2code.ecommerce.entity.*;
import com.luv2code.ecommerce.service.AuthService;
import com.luv2code.ecommerce.dao.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class OrderServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    private User testUser;

    @BeforeEach
    public void setUp() {
        // Clean up test data in correct order to avoid foreign key constraint violations
        userRepository.deleteAll();  // This will cascade delete orders and order_items
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();

        // Create test user
        testUser = authService.registerUser("ordertest", "ordertest@example.com", "password123", "Order", "Test");
    }

    @Test
    public void testCreateOrder() {
        Order order = new Order();
        order.setUser(testUser);
        order.setOrderTrackingNumber("TEST-001");
        order.setTotalPrice(new BigDecimal("99.99"));
        order.setTotalQuantity(1);
        order.setStatus("pending");
        order.setCustomerEmail(testUser.getEmail());
        order.setBillingAddress("123 Main St");
        order.setShippingAddress("456 Oak Ave");
        order.setOrderItems(new HashSet<>());

        Order savedOrder = orderRepository.save(order);

        assertNotNull(savedOrder);
        assertNotNull(savedOrder.getId());
        assertEquals("TEST-001", savedOrder.getOrderTrackingNumber());
        assertEquals(testUser.getId(), savedOrder.getUser().getId());
        assertEquals("pending", savedOrder.getStatus());
    }

    @Test
    public void testOrderToUserRelationship() {
        Order order = new Order();
        order.setUser(testUser);
        order.setOrderTrackingNumber("TEST-002");
        order.setTotalPrice(new BigDecimal("199.99"));
        order.setTotalQuantity(2);
        order.setStatus("pending");
        order.setCustomerEmail(testUser.getEmail());
        order.setOrderItems(new HashSet<>());

        // Manually sync bidirectional relationship
        testUser.getOrders().add(order);
        Order savedOrder = orderRepository.save(order);
        userRepository.save(testUser);

        User retrievedUser = userRepository.findById(testUser.getId()).orElse(null);
        assertNotNull(retrievedUser);
        assertTrue(retrievedUser.getOrders().stream()
                .anyMatch(o -> o.getId().equals(savedOrder.getId())));
    }

    @Test
    public void testFindOrderByUser() {
        Order order = new Order();
        order.setUser(testUser);
        order.setOrderTrackingNumber("TEST-003");
        order.setTotalPrice(new BigDecimal("99.99"));
        order.setTotalQuantity(1);
        order.setStatus("pending");
        order.setCustomerEmail(testUser.getEmail());
        order.setOrderItems(new HashSet<>());

        orderRepository.save(order);

        java.util.List<Order> orders = orderRepository.findAll();
        assertTrue(orders.stream()
                .anyMatch(o -> o.getUser().getId().equals(testUser.getId())));
    }

    @Test
    public void testOrderStatusUpdate() {
        Order order = new Order();
        order.setUser(testUser);
        order.setOrderTrackingNumber("TEST-004");
        order.setTotalPrice(new BigDecimal("99.99"));
        order.setTotalQuantity(1);
        order.setStatus("pending");
        order.setCustomerEmail(testUser.getEmail());
        order.setOrderItems(new HashSet<>());

        Order savedOrder = orderRepository.save(order);
        assertEquals("pending", savedOrder.getStatus());

        savedOrder.setStatus("completed");
        Order updatedOrder = orderRepository.save(savedOrder);

        assertEquals("completed", updatedOrder.getStatus());
    }
}
