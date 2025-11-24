package com.decorom.backend.dto;

import com.decorom.backend.entity.Order;
import lombok.Data;

@Data
public class CheckoutRequest {
    private String category;
    private String material;
    private String size;
    private double totalSqInch;
    private double frontendPrice;
    private boolean lightingIncluded;
    private boolean fittingIncluded;
    private Order.CustomerAddress customerAddress;
}
