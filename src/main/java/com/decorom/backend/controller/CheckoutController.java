package com.decorom.backend.controller;

import com.decorom.backend.dto.CheckoutRequest;
import com.decorom.backend.entity.Order;
import com.decorom.backend.service.EmailService;
import com.decorom.backend.service.OrderService;
import com.decorom.backend.service.PricingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/checkout")
@RequiredArgsConstructor
@Slf4j
public class CheckoutController {

    private final PricingService pricingService;
    private final OrderService orderService;
    private final EmailService emailService;

    @Value("${phonepe.merchant.id}")
    private String merchantId;

    @Value("${phonepe.salt.key}")
    private String saltKey;

    @Value("${phonepe.salt.index}")
    private String saltIndex;

    @Value("${phonepe.base.url}")
    private String baseUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<?> checkout(@RequestBody CheckoutRequest request, HttpServletRequest servletRequest) {
        log.info("Received checkout request: {}", request);

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

        log.info("Order created successfully with ID: {}. Payment Status: {}", savedOrder.getId(),
                savedOrder.getPaymentStatus());

        // 6. Response
        if (isPriceValid) {
            try {
                String paymentUrl = initiatePhonePePayment(savedOrder);
                Map<String, String> response = new HashMap<>();
                response.put("message", "Order Created");
                response.put("orderId", savedOrder.getId().toString());
                response.put("paymentUrl", paymentUrl);
                return ResponseEntity.ok(response);
            } catch (Exception e) {
                log.error("Failed to initiate PhonePe payment", e);
                return ResponseEntity.internalServerError().body("Failed to initiate payment: " + e.getMessage());
            }
        } else {
            log.warn("Price mismatch detected for Order ID: {}. Frontend: {}, Server: {}", savedOrder.getId(),
                    request.getFrontendPrice(), serverPrice);
            return ResponseEntity.badRequest().body("Price mismatch detected. Order flagged.");
        }
    }

    private String initiatePhonePePayment(Order order) throws Exception {
        // Construct Payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("merchantId", merchantId);
        payload.put("merchantTransactionId", order.getId().toString());
        payload.put("merchantUserId", "USER_" + order.getId());
        payload.put("amount", (long) (order.getServerCalculatedPrice() * 100)); // Amount in paise
        payload.put("redirectUrl", "http://localhost:5173/payment-success?orderId=" + order.getId()); // Frontend
                                                                                                      // Success Page
        payload.put("redirectMode", "REDIRECT");
        payload.put("callbackUrl", "https://your-ngrok-url.ngrok-free.app/payment-notification"); // Needs public URL
                                                                                                  // for callback
        payload.put("paymentInstrument", Map.of("type", "PAY_PAGE"));

        String jsonPayload = objectMapper.writeValueAsString(payload);
        String base64Payload = java.util.Base64.getEncoder().encodeToString(jsonPayload.getBytes());

        // Calculate Checksum
        String stringToHash = base64Payload + "/pg/v1/pay" + saltKey;
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(stringToHash.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1)
                hexString.append('0');
            hexString.append(hex);
        }
        String checksum = hexString.toString() + "###" + saltIndex;

        // Make Request
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        headers.set("X-VERIFY", checksum);

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("request", base64Payload);

        org.springframework.http.HttpEntity<Map<String, String>> requestEntity = new org.springframework.http.HttpEntity<>(
                requestBody, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl + "/pg/v1/pay", requestEntity, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Map data = (Map) response.getBody().get("data");
            Map instrumentResponse = (Map) data.get("instrumentResponse");
            Map redirectInfo = (Map) instrumentResponse.get("redirectInfo");
            return (String) redirectInfo.get("url");
        } else {
            throw new RuntimeException("PhonePe API returned error: " + response.getStatusCode());
        }
    }
}
