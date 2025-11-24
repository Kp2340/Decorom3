package com.decorom.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String category;
    private String material;
    private String size; // e.g., "10x12"
    private double totalSqInch;

    private double frontendPrice;
    private double serverCalculatedPrice;
    private boolean isPriceValid;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    @Embedded
    private CustomerAddress customerAddress;

    private String userIP;

    @CreationTimestamp
    private LocalDateTime createdAt;

    // Additional fields for pricing logic context if needed (lighting, fitting)
    private boolean lightingIncluded;
    private boolean fittingIncluded;

    public enum PaymentStatus {
        PENDING, SUCCESS, FAILED
    }

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerAddress {
        private String fullName;
        private String email;
        private String phone;
        private String street;
        private String city;
        private String state;
        private String zipCode;
    }
}
