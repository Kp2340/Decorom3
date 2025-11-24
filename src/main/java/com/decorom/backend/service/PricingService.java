package com.decorom.backend.service;

import org.springframework.stereotype.Service;

@Service
public class PricingService {

    public double calculatePrice(String material, double totalSqInch, boolean lightingIncluded,
            boolean fittingIncluded) {
        double baseRate = getBaseRate(material, totalSqInch);
        double price = baseRate * totalSqInch;

        // Lighting Logic
        if (lightingIncluded) {
            if (isMetal(material)) {
                price *= 1.6;
            } else {
                price *= 2.0; // Acrylic, ACP, Wood
            }
        }

        // Fitting Logic
        if (fittingIncluded) {
            price += 500;
        }

        return price;
    }

    private double getBaseRate(String material, double totalSqInch) {
        String mat = material.toLowerCase();

        if (mat.contains("acrylic") || mat.contains("wood")) {
            if (totalSqInch <= 100)
                return 13.0;
            if (totalSqInch <= 225)
                return 11.5;
            return 10.0;
        } else if (mat.contains("acp")) {
            if (totalSqInch <= 100)
                return 14.0;
            if (totalSqInch <= 225)
                return 12.0;
            return 10.0;
        } else if (mat.contains("ss") || mat.contains("ms") || mat.contains("metal")) {
            if (totalSqInch <= 100)
                return 30.0;
            if (totalSqInch <= 225)
                return 25.0;
            return 20.0;
        }

        // Default fallback or throw exception
        throw new IllegalArgumentException("Unknown material: " + material);
    }

    private boolean isMetal(String material) {
        String mat = material.toLowerCase();
        return mat.contains("ss") || mat.contains("ms") || mat.contains("metal");
    }
}
