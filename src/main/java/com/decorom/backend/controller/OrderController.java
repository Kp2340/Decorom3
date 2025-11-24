package com.decorom.backend.controller;

import com.decorom.backend.entity.Order;
import com.decorom.backend.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrderDetails(@PathVariable UUID orderId) {
        log.info("Fetching order details for Order ID: {}", orderId);

        return orderService.getOrderById(orderId)
                .map(order -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("orderId", order.getId());
                    response.put("category", order.getCategory());
                    response.put("material", order.getMaterial());
                    response.put("size", order.getSize());
                    response.put("totalAmount", order.getServerCalculatedPrice());
                    response.put("paymentStatus", order.getPaymentStatus());
                    response.put("customerAddress", order.getCustomerAddress());
                    response.put("createdAt", order.getCreatedAt());
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
