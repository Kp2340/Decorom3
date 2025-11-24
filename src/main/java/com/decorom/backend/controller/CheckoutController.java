package com.decorom.backend.controller;

import com.decorom.backend.dto.CheckoutRequest;
import com.decorom.backend.entity.Order;
import com.decorom.backend.service.EmailService;
import com.decorom.backend.service.OrderService;
import com.decorom.backend.service.PricingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final PricingService pricingService;
    private final OrderService orderService;
    private final EmailService emailService;

    @PostMapping
    public ResponseEntity<?> checkout(@RequestBody CheckoutRequest request, HttpServletRequest servletRequest) {
        // 1. Calculate Server Price
        double serverPrice = pricingService.calculatePrice(
                request.getMaterial(),
                request.getTotalSqInch(),
                request.isLightingIncluded(),
                request.isFittingIncluded());

        // 2. Validate Price (Allowing 1.0 delta for floating point errors)
        boolean isPriceValid = Math.abs(serverPrice - request.getFrontendPrice()) < 1.0;

        // 3. Create Order Entity
        Order order = new Order();
        order.setCategory(request.getCategory());
        order.setMaterial(request.getMaterial());
        order.setSize(request.getSize());
        order.setTotalSqInch(request.getTotalSqInch());
        order.setFrontendPrice(request.getFrontendPrice());
        order.setServerCalculatedPrice(serverPrice);
        order.setPriceValid(isPriceValid);
        order.setPaymentStatus(Order.PaymentStatus.PENDING);
        order.setCustomerAddress(request.getCustomerAddress());
        order.setUserIP(servletRequest.getRemoteAddr());
        order.setLightingIncluded(request.isLightingIncluded());
        order.setFittingIncluded(request.isFittingIncluded());

        // 4. Persist Order
        Order savedOrder = orderService.saveOrder(order);

        // 5. Notify
        emailService.sendOrderAlert(savedOrder);

        // 6. Response
        if (isPriceValid) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Order Created");
            response.put("orderId", savedOrder.getId().toString());
            response.put("paymentUrl",
                    "https://mercury-uat.phonepe.com/transact/pay?token=MOCK_TOKEN_" + savedOrder.getId()); // Mock URL
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body("Price mismatch detected. Order flagged.");
        }
    }
}
