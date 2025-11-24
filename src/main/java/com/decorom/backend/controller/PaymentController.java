package com.decorom.backend.controller;

import com.decorom.backend.entity.Order;
import com.decorom.backend.service.EmailService;
import com.decorom.backend.service.OrderService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final OrderService orderService;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    @Value("${phonepe.salt.key}")
    private String saltKey;

    @Value("${phonepe.salt.index}")
    private String saltIndex;

    @PostMapping("/payment-notification")
    public ResponseEntity<?> handlePaymentCallback(
            @RequestBody Map<String, String> payload,
            @RequestHeader("X-VERIFY") String xVerify) {

        String response = payload.get("response"); // Base64 encoded JSON

        // 1. Verify Checksum
        // X-VERIFY = SHA256(response + saltKey) + ### + saltIndex
        if (!verifyChecksum(response, xVerify)) {
            return ResponseEntity.badRequest().body("Invalid Checksum");
        }

        try {
            // 2. Decode and Parse
            String decodedResponse = new String(Base64.getDecoder().decode(response), StandardCharsets.UTF_8);
            JsonNode jsonNode = objectMapper.readTree(decodedResponse);

            // Assuming the transactionId is the Order ID
            String transactionId = jsonNode.get("data").get("merchantTransactionId").asText();
            String code = jsonNode.get("code").asText();

            if ("PAYMENT_SUCCESS".equals(code)) {
                // 3. Update DB
                UUID orderId = UUID.fromString(transactionId);
                Order order = orderService.getOrderById(orderId)
                        .orElseThrow(() -> new RuntimeException("Order not found"));

                order.setPaymentStatus(Order.PaymentStatus.SUCCESS);
                orderService.saveOrder(order);

                // 4. Final Confirmation
                emailService.sendPaymentConfirmation(order);
            } else {
                // Handle failure if needed
                UUID orderId = UUID.fromString(transactionId);
                orderService.getOrderById(orderId).ifPresent(order -> {
                    order.setPaymentStatus(Order.PaymentStatus.FAILED);
                    orderService.saveOrder(order);
                });
            }

            return ResponseEntity.ok("Received");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error processing callback");
        }
    }

    private boolean verifyChecksum(String response, String xVerify) {
        try {
            String dataToHash = response + saltKey;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(dataToHash.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }

            String calculatedChecksum = hexString.toString() + "###" + saltIndex;
            return calculatedChecksum.equals(xVerify);
        } catch (Exception e) {
            return false;
        }
    }
}
